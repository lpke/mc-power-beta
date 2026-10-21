package local.luke.power.world;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared ownership for offline save edits and streamed world operations. */
public final class SavedWorldAccess {
  public static final String INCOMPLETE = "power-beta-incomplete.json";
  private static final Map<Path, Lease> OWNERS = new HashMap<>();
  private final Path game, world;
  private final BooleanSupplier unloaded;

  public SavedWorldAccess(Path game, String folder, BooleanSupplier unloaded) throws IOException {
    this.game = game.toAbsolutePath().normalize();
    this.unloaded = Objects.requireNonNull(unloaded);
    Path saves = this.game.resolve("saves");
    try { world = saves.resolve(folder).normalize(); }
    catch (InvalidPathException | NullPointerException invalid) { throw new IOException("Invalid world folder.", invalid); }
    if (folder.isBlank() || !saves.equals(world.getParent())) throw new IOException("Invalid world folder.");
    safe(world);
    if (!Files.isDirectory(world, LinkOption.NOFOLLOW_LINKS)) throw new IOException("The saved world folder is missing.");
    checkIdle();
  }

  public Path game() { return game; }
  public Path world() { return world; }
  public Path level() { return world.resolve("level.dat"); }
  public Path old() { return world.resolve("level.dat_old"); }

  public void checkIdle() throws IOException { checkIdle(null); }
  public void checkIdle(Lease lease) throws IOException {
    if (!unloaded.getAsBoolean()) throw new IOException("Leave the open world before changing saved worlds.");
    safe(world);
    if (lease != null && (lease.closed || !lease.game.equals(game))) throw new IOException("The world-operation lease is no longer valid.");
    requireAvailable(game, lease);
    assertNoOtherClient(game);
  }

  public Lease acquire() throws IOException {
    synchronized (OWNERS) {
      checkIdle();
      Path lock = lockPath(game);
      safe(lock); Files.createDirectories(lock.getParent()); safe(lock);
      FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      try {
        FileLock held = channel.tryLock();
        if (held == null) throw new IOException("Another world operation is using this instance.");
        Lease lease = new Lease(game, channel, held);
        OWNERS.put(game, lease);
        try { checkIdle(lease); }
        catch (IOException failure) { lease.close(); throw failure; }
        return lease;
      } catch (OverlappingFileLockException busy) {
        channel.close(); throw new IOException("Another world operation is using this instance.", busy);
      } catch (IOException | RuntimeException failure) { channel.close(); throw failure; }
    }
  }

  /** World opening must check this before changing session locks or metadata. */
  public static void requireAvailable(Path game) throws IOException {
    requireAvailable(game.toAbsolutePath().normalize(), null);
  }

  public static void requireOpenable(Path game, String folder) throws IOException {
    requireAvailable(game);
    Path saves = game.toAbsolutePath().normalize().resolve("saves"), world;
    try { world = saves.resolve(folder).normalize(); }
    catch (InvalidPathException | NullPointerException invalid) { throw new IOException("Invalid world folder.", invalid); }
    if (!saves.equals(world.getParent())) throw new IOException("Invalid world folder.");
    safe(world);
    if (Files.exists(world.resolve(INCOMPLETE), LinkOption.NOFOLLOW_LINKS))
      throw new IOException("This copied world has an unfinished publication. Keep its recovery manifest and restore a verified backup before opening it.");
  }

  private static void requireAvailable(Path game, Lease expected) throws IOException {
    synchronized (OWNERS) {
      Lease owner = OWNERS.get(game);
      if (owner != null) {
        if (owner == expected && !owner.closed) return;
        throw new IOException("Finish or cancel the current world operation first.");
      }
    }
    Path lock = lockPath(game); safe(lock);
    if (!Files.exists(lock, LinkOption.NOFOLLOW_LINKS)) return;
    if (!Files.isRegularFile(lock, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid world-operation lock.");
    try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.READ)) {
      try (FileLock held = channel.tryLock(0, Long.MAX_VALUE, true)) {
        if (held == null) throw new IOException("Another world operation is using this instance.");
      } catch (OverlappingFileLockException busy) {
        throw new IOException("Another world operation is using this instance.", busy);
      }
    }
  }

  private static Path lockPath(Path game) { return game.resolve("power-beta-data/world-operations/instance.lock"); }

  /** Reject symbolic links in every ancestor, including an instance reached through a link. */
  public static void safe(Path path) throws IOException {
    for (Path part = path.toAbsolutePath().normalize(); part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("World operation path contains a symbolic link.");
  }

  public static byte[] metadataBytes(Path path) throws IOException {
    safe(path);
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 4L * 1024 * 1024)
      throw new IOException("World metadata is not a supported file or exceeds 4 MiB.");
    return Files.readAllBytes(path);
  }

  private static void assertNoOtherClient(Path game) throws IOException {
    Path proc = Path.of("/proc");
    if (Files.isDirectory(proc)) {
      try (var processes = Files.list(proc)) {
        for (Path process : processes.filter(p -> p.getFileName().toString().matches("[0-9]+")).toList()) {
          try {
            if (Long.parseLong(process.getFileName().toString()) == ProcessHandle.current().pid()) continue;
            if (Files.readString(process.resolve("comm")).strip().equals("java")
                && process.resolve("cwd").toRealPath().equals(game))
              throw new IOException("Another client is using this instance. Close it before changing saved worlds.");
          } catch (NoSuchFileException | AccessDeniedException ignored) { }
        }
      }
      return;
    }
    try (var processes = ProcessHandle.allProcesses()) {
      for (ProcessHandle process : processes.toList()) {
        if (process.pid() == ProcessHandle.current().pid()) continue;
        String[] arguments = process.info().arguments().orElse(new String[0]);
        for (int i = 0; i < arguments.length; i++) {
          String argument = arguments[i];
          boolean same = argument.equals("--gameDir") && i + 1 < arguments.length && samePath(arguments[i + 1], game)
              || argument.startsWith("-Duser.dir=") && samePath(argument.substring(11), game);
          if (argument.startsWith("-Djava.library.path=")) for (String path : argument.substring(20).split(java.util.regex.Pattern.quote(File.pathSeparator)))
            same |= samePath(path, game.resolveSibling("natives"));
          if (same) throw new IOException("Another client is using this instance. Close it before changing saved worlds.");
        }
      }
    }
  }

  private static boolean samePath(String value, Path expected) {
    try { return Path.of(value).toAbsolutePath().normalize().equals(expected); }
    catch (InvalidPathException ignored) { return false; }
  }

  public static final class Lease implements AutoCloseable {
    private final Path game;
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;
    private Lease(Path game, FileChannel channel, FileLock lock) { this.game = game; this.channel = channel; this.lock = lock; }
    public void close() throws IOException {
      synchronized (OWNERS) {
        if (closed) return;
        closed = true;
        try { lock.release(); }
        finally { try { channel.close(); } finally { OWNERS.remove(game, this); } }
      }
    }
  }
}
