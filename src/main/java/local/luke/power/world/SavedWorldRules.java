package local.luke.power.world;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.config.FileTransaction;
import local.luke.power.rules.RuleSet;
import net.minecraft.nbt.*;

/** Edits an unopened world's rules without constructing a world or rewriting player data. */
public final class SavedWorldRules {
  private final Path game, level, old;
  private final BooleanSupplier unloaded;
  private byte[] expectedLevel, expectedOld;
  private RuleSet rules;
  private final String name;

  public SavedWorldRules(Path game, String folder, BooleanSupplier unloaded) throws IOException {
    this.game = game.toAbsolutePath().normalize();
    this.unloaded = Objects.requireNonNull(unloaded);
    Path saves = this.game.resolve("saves"), world = saves.resolve(folder).normalize();
    if (!saves.equals(world.getParent()) || folder.isBlank()) throw new IOException("Invalid world folder.");
    level = world.resolve("level.dat"); old = world.resolve("level.dat_old");
    checkIdle();
    expectedLevel = bytes(level); expectedOld = bytes(old);
    NbtCompound data = WorldRuleFiles.read(source()).getCompound("Data");
    rules = WorldRuleData.read(data);
    name = data.getString("LevelName").isBlank() ? folder : data.getString("LevelName");
  }

  public String name() { return name; }
  public RuleSet rules() { return rules.copy(); }

  public void validate() throws IOException {
    checkIdle();
    if (!Arrays.equals(expectedLevel, bytes(level)) || !Arrays.equals(expectedOld, bytes(old)))
      throw new IOException("World metadata changed. Reopen Edit game rules before saving.");
  }

  public void save(RuleSet next) throws IOException {
    next = next.copy();
    validate();
    if (rules.equals(next)) return;
    byte[] saved;
    try (FileTransaction transaction = FileTransaction.begin(game, List.of(level, old))) {
      validate();
      NbtCompound root = WorldRuleFiles.read(source());
      WorldRuleData.write(root.getCompound("Data"), next);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      NbtIo.writeCompressed(root, output); saved = output.toByteArray();
      validate();
      FileTransaction.atomicWrite(level, saved);
      checkIdle();
      if (!Arrays.equals(saved, bytes(level)) || !WorldRuleData.read(WorldRuleFiles.read(level).getCompound("Data")).equals(next))
        throw new IOException("Saved game rules could not be verified. The original metadata will be restored.");
      transaction.commit();
    }
    expectedLevel = saved;
    rules = next.copy();
  }

  private Path source() throws IOException {
    if (Files.isRegularFile(level)) return level;
    if (Files.isRegularFile(old)) return old;
    throw new IOException("World metadata is missing. Restore level.dat from a backup.");
  }
  private byte[] bytes(Path path) throws IOException {
    if (!Files.exists(path)) return null;
    if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("World metadata exceeds 4 MiB.");
    return Files.readAllBytes(path);
  }
  private void checkIdle() throws IOException {
    if (!unloaded.getAsBoolean()) throw new IOException("Leave the open world before editing saved game rules.");
    for (Path path : List.of(level, old)) for (Path part = path; part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("World metadata path contains a symbolic link.");
    Path proc = Path.of("/proc");
    if (!Files.isDirectory(proc)) {
      // Prism supplies the instance's native-library directory even without --gameDir.
      try (var processes = ProcessHandle.allProcesses()) {
        for (ProcessHandle process : processes.toList()) {
          if (process.pid() == ProcessHandle.current().pid()) continue;
          String[] arguments = process.info().arguments().orElse(new String[0]);
          for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            boolean same = argument.equals("--gameDir") && i + 1 < arguments.length && samePath(arguments[i+1], game)
                || argument.startsWith("-Duser.dir=") && samePath(argument.substring(11), game);
            if (argument.startsWith("-Djava.library.path=")) for (String path : argument.substring(20).split(java.util.regex.Pattern.quote(File.pathSeparator)))
              same |= samePath(path, game.resolveSibling("natives"));
            if (same) throw new IOException("Another client is using this instance. Close it before editing game rules.");
          }
        }
      }
      return;
    }
    try (var processes = Files.list(proc)) {
      for (Path process : processes.filter(p -> p.getFileName().toString().matches("[0-9]+")).toList()) {
        try {
          if (Long.parseLong(process.getFileName().toString()) == ProcessHandle.current().pid()) continue;
          if (Files.readString(process.resolve("comm")).strip().equals("java")
              && process.resolve("cwd").toRealPath().equals(game))
            throw new IOException("Another client is using this instance. Close it before editing game rules.");
        } catch (NoSuchFileException | AccessDeniedException ignored) { }
      }
    }
  }
  private static boolean samePath(String value, Path expected) {
    try { return Path.of(value).toAbsolutePath().normalize().equals(expected); }
    catch (InvalidPathException ignored) { return false; }
  }
}
