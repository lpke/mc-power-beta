package local.luke.power.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.zip.*;
import local.luke.power.config.FileTransaction;
import local.luke.power.storage.DurableFiles;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.NbtCompound;

/** One bounded worker streams an unopened save, verifies it, then publishes its new name. */
public final class WorldSnapshots {
  public enum Action { BACKUP, DUPLICATE }
  public record BackupOptions(boolean maps, boolean powerConfig, boolean metadataBackups) {
    public static final BackupOptions ALL = new BackupOptions(true, true, true);
  }
  public record Result(Path path, List<String> warnings) {
    public Result { warnings = List.copyOf(warnings); }
  }
  @FunctionalInterface interface Faults { void at(String phase, Path path) throws IOException; }
  @FunctionalInterface interface Progress { void set(String phase, long done, long total); }
  private static final Faults NORMAL = (phase, path) -> {};
  private static final String MANIFEST = "power-beta-backup.json";
  private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(1), task -> { Thread thread = new Thread(task, "Power Beta world storage"); thread.setDaemon(true); return thread; },
      new ThreadPoolExecutor.AbortPolicy());
  private static Task active;
  private WorldSnapshots() {}

  public static synchronized Task start(Path game, String folder, Action action, String name, BooleanSupplier unloaded) throws IOException {
    return start(game, folder, action, name, BackupOptions.ALL, unloaded);
  }

  public static synchronized Task start(Path game, String folder, Action action, String name, BackupOptions options,
      BooleanSupplier unloaded) throws IOException {
    if (active != null && !active.finished) throw new IOException("Finish or cancel the current world operation first.");
    name = validName(name);
    SavedWorldAccess access = new SavedWorldAccess(game, folder, unloaded);
    Objects.requireNonNull(options);
    Task task = new Task(access, Objects.requireNonNull(action), name, options, access.acquire());
    active = task;
    try { WORKER.execute(task::run); }
    catch (RejectedExecutionException busy) { active = null; task.lease.close(); throw new IOException("World storage is busy. Try again when it finishes.", busy); }
    return task;
  }

  public static String validName(String name) throws IOException {
    if (name == null || name.strip().isEmpty() || name.strip().length() > 128 || name.chars().anyMatch(Character::isISOControl))
      throw new IOException("Use a world name of 1 to 128 characters.");
    return name.strip();
  }

  public static final class Task {
    private final SavedWorldAccess access;
    private final Action action;
    private final String name;
    private final BackupOptions options;
    private final SavedWorldAccess.Lease lease;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile boolean finished;
    private volatile String phase = "Preparing world files", error = "";
    private volatile long done, total;
    private volatile Result result;
    private Task(SavedWorldAccess access, Action action, String name, BackupOptions options, SavedWorldAccess.Lease lease) {
      this.access = access; this.action = action; this.name = name; this.options = options; this.lease = lease;
    }
    public void cancel() { cancelled.set(true); }
    public boolean finished() { return finished; }
    public boolean cancelling() { return cancelled.get() && !finished; }
    public String phase() { return phase; }
    public String error() { return error; }
    public Result result() { return result; }
    public double progress() { return total > 0 ? Math.min(1, (double)done / total) : 0; }
    private void run() {
      try {
        PowerConfig.flushPending();
        local.luke.power.storage.JournalQueue.awaitQueued();
        local.luke.power.map.MapIO.awaitQueued();
        result = execute(access, lease, action, name, options, cancelled::get,
            (message, current, count) -> { phase = message; done = current; total = count; }, NORMAL);
        phase = action == Action.BACKUP ? "Backup created" : "World duplicated";
      } catch (IOException | RuntimeException failure) {
        error = Objects.toString(failure.getMessage(), "World operation failed. Original world preserved.");
      } finally {
        try { lease.close(); } catch (IOException failure) { error = "World-operation lock could not be released: " + failure.getMessage(); }
        finished = true;
      }
    }
  }

  static Result execute(SavedWorldAccess access, Action action, String name, BooleanSupplier cancelled,
      Progress progress, Faults faults) throws IOException {
    return execute(access, action, name, BackupOptions.ALL, cancelled, progress, faults);
  }
  static Result execute(SavedWorldAccess access, Action action, String name, BackupOptions options, BooleanSupplier cancelled,
      Progress progress, Faults faults) throws IOException {
    Objects.requireNonNull(options);
    return execute(access, access.acquire(), action, name, options, cancelled, progress, faults);
  }
  private static Result execute(SavedWorldAccess access, SavedWorldAccess.Lease ownedLease, Action action, String name,
      BackupOptions options, BooleanSupplier cancelled, Progress progress, Faults faults) throws IOException {
    Path root = access.game().resolve("power-beta-data/world-operations");
    Path stage = null, publishedMaps = null;
    boolean worldPublished = false;
    IOException primaryFailure = null;
    try (var lease = ownedLease) {
      name = validName(name);
      SavedWorldAccess.safe(root); Files.createDirectories(root); SavedWorldAccess.safe(root);
      WorldSnapshotFiles.check(cancelled);
      stage = Files.createTempDirectory(root, "staging-");
      DurableFiles.syncDirectory(root);
      WorldOwnedData.Owner owner = WorldOwnedData.owner(access.world());
      WorldOwnedData.Discovery external = discover(access.game(), owner, action, options);
      List<WorldSnapshotFiles.Source> sources = WorldSnapshotFiles.collect(access.game(), access.world(), external, cancelled);
      if (action == Action.DUPLICATE) checkDuplicate(access.world(), owner, external, sources);
      faults.at("collected", stage); access.checkIdle(lease);
      long bytes = total(sources);
      long reserve = Math.min(bytes, 64L * 1024 * 1024);
      if (Files.getFileStore(stage).getUsableSpace() < (bytes > Long.MAX_VALUE - reserve ? Long.MAX_VALUE : bytes + reserve))
        throw new IOException("There is not enough free space to verify this world copy.");
      Map<String, WorldSnapshotFiles.Proof> proofs = new LinkedHashMap<>();
      Path destination;
      if (action == Action.BACKUP) {
        Path zip = stage.resolve("backup.zip.partial");
        byte[] manifest = writeZip(zip, sources, proofs, owner, external, access.game(), cancelled, progress, faults);
        faults.at("copied", zip);
        verifyZip(zip, sources, proofs, manifest, cancelled);
        faults.at("verified-copy", zip);
        Path backups = access.game().resolve("backups/power-beta");
        SavedWorldAccess.safe(backups); Files.createDirectories(backups); SavedWorldAccess.safe(backups);
        DurableFiles.syncDirectory(backups.getParent()); DurableFiles.syncDirectory(access.game());
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        destination = available(backups, date + "_" + fileName(name), ".zip");
        faults.at("before-publish", destination); WorldSnapshotFiles.check(cancelled);
        validateSources(access, lease, owner, action, options, sources, proofs, cancelled, progress);
        rename(zip, destination); faults.at("backup-renamed", destination);
        DurableFiles.syncDirectory(destination.getParent());
      } else {
        String id = UUID.randomUUID().toString();
        Path copy = stage.resolve("world"), maps = stage.resolve("maps"); Files.createDirectory(copy);
        FileTransaction.atomicWrite(copy.resolve(SavedWorldAccess.INCOMPLETE), marker(id, name));
        copyWorld(copy, maps, sources, proofs, cancelled, progress, faults);
        validatePlayers(copy, cancelled);
        rewriteMetadata(copy, id, name, cancelled);
        JsonObject manifest = manifest(owner, external, access.game(), sources, proofs);
        manifest.addProperty("duplicateWorldId", id);
        Map<Path, WorldSnapshotFiles.Proof> payload = payload(copy, maps, cancelled);
        JsonArray contents = new JsonArray();
        for (var entry : payload.entrySet()) {
          JsonObject file = new JsonObject(); file.addProperty("path", stage.relativize(entry.getKey()).toString().replace(File.separatorChar, '/'));
          file.addProperty("bytes", entry.getValue().bytes()); file.addProperty("sha256", entry.getValue().sha256()); contents.add(file);
        }
        manifest.add("payload", contents);
        byte[] manifestBytes = manifest.toString().getBytes(StandardCharsets.UTF_8);
        Path manifestPath = copy.resolve("power-beta-copy.json");
        FileTransaction.atomicWrite(manifestPath, manifestBytes);
        syncTree(copy, cancelled);
        if (Files.exists(maps)) syncTree(maps, cancelled);
        faults.at("verified-copy", copy);
        for (var entry : payload.entrySet())
          if (!entry.getValue().equals(WorldSnapshotFiles.hash(entry.getKey(), cancelled))) throw new IOException("Copied world contents changed during verification.");
        if (!Arrays.equals(manifestBytes, Files.readAllBytes(manifestPath))) throw new IOException("Copy manifest could not be verified.");
        destination = available(access.game().resolve("saves"), fileName(name), "");
        faults.at("before-publish", destination); WorldSnapshotFiles.check(cancelled);
        validateSources(access, lease, owner, action, options, sources, proofs, cancelled, progress);
        if (Files.exists(maps)) {
          Path target = access.game().resolve("power-beta-data/maps")
              .resolve(UUID.nameUUIDFromBytes(("world:" + id).getBytes(StandardCharsets.UTF_8)).toString());
          SavedWorldAccess.safe(target); Files.createDirectories(target.getParent());
          if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("The new map identity is already in use.");
          rename(maps, target); publishedMaps = target; faults.at("maps-renamed", target);
          DurableFiles.syncDirectory(target.getParent()); faults.at("maps-published", target);
        }
        rename(copy, destination); worldPublished = true; faults.at("world-renamed", destination);
        DurableFiles.syncDirectory(destination.getParent());
        faults.at("published", destination);
        // The marker remains recoverable if durability fails after the atomic rename.
        DurableFiles.syncDirectory(destination);
        Files.delete(destination.resolve(SavedWorldAccess.INCOMPLETE));
        DurableFiles.syncDirectory(destination);
        DurableFiles.syncDirectory(destination.getParent());
      }
      return new Result(destination, external.warnings());
    } catch (IOException failure) { primaryFailure = failure; throw failure; }
    finally {
      try {
        if (publishedMaps != null && !worldPublished) removeOwnedTree(publishedMaps);
        if (stage != null) removeStage(root, stage);
      } catch (IOException cleanup) {
        if (primaryFailure != null) primaryFailure.addSuppressed(cleanup); else throw cleanup;
      }
    }
  }

  private static WorldOwnedData.Discovery discover(Path game, WorldOwnedData.Owner owner, Action action,
      BackupOptions options) throws IOException {
    var discovered = WorldOwnedData.discover(game, owner);
    if (action != Action.BACKUP) return discovered;
    List<WorldOwnedData.External> files = new ArrayList<>();
    for (var file : discovered.files()) {
      if (!options.maps() && Set.of("maps", "unclassified-maps").contains(file.kind())) continue;
      // Pending and unclassified evidence always stays with the native world data.
      if (!options.metadataBackups() && !file.pending() && !file.uncertain()
          && Set.of("settings-backups", "world-metadata-backups").contains(file.kind())) continue;
      files.add(file);
    }
    if (options.powerConfig()) {
      Path config = game.resolve("config/power-beta.json"); SavedWorldAccess.safe(config);
      if (Files.exists(config, LinkOption.NOFOLLOW_LINKS)) {
        if (!Files.isRegularFile(config, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Power Beta config is not a regular file.");
        files.add(new WorldOwnedData.External(config, "power-config", false, false));
      }
    }
    List<String> warnings = discovered.warnings().stream()
        .filter(warning -> options.maps() || !warning.startsWith("The save has no readable stable world ID.")).toList();
    return new WorldOwnedData.Discovery(files, warnings);
  }

  private static void checkDuplicate(Path world, WorldOwnedData.Owner owner, WorldOwnedData.Discovery external,
      List<WorldSnapshotFiles.Source> sources) throws IOException {
    if (owner.ids().size() > 1) throw new IOException("World metadata has conflicting world IDs. Restore matching metadata before duplicating it.");
    if (Files.exists(world.resolve(SavedWorldAccess.INCOMPLETE))) throw new IOException("Finish recovering this incomplete world copy before duplicating it.");
    if (!Files.isRegularFile(world.resolve("level.dat")) && !Files.isRegularFile(world.resolve("level.dat_old")))
      throw new IOException("World metadata is missing. A raw backup is still available.");
    if (external.files().stream().anyMatch(WorldOwnedData.External::pending))
      throw new IOException("This world has an interrupted recovery or metadata save. Keep a backup and resolve recovery before duplicating it.");
    Set<String> identities = new HashSet<>(owner.ids());
    int metadataFiles = 0;
    for (var source : sources) {
      String file = source.path().getFileName().toString();
      if (source.kind().equals("world") && !source.stamp().directory()
          && Set.of("level.dat", "level.dat_old", "level.dat_new").contains(file)) {
        if (++metadataFiles > 256) throw new IOException("Too many metadata files for a safe world copy.");
        NbtCompound data = WorldRuleFiles.read(source.path()).getCompound("Data");
        if (data.contains("PowerBetaCommandWorldId")) {
          String id = data.getString("PowerBetaCommandWorldId");
          try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
          catch (IllegalArgumentException invalid) { throw new IOException("World metadata has an invalid stable identity. A raw backup remains available.", invalid); }
          identities.add(id);
        }
        if (identities.size() > 1) throw new IOException("World dimensions and fallback metadata have conflicting identities. Restore matching metadata before duplicating it.");
      }
      if (!source.kind().equals("world") || source.stamp().directory()
          || !(file.equals("power-beta-carry.dat") || file.equals("power-beta-carry"))) continue;
      if (source.stamp().size() > 4L * 1024 * 1024) throw new IOException("Carried-container recovery data is too large to validate.");
      try {
        JsonObject carry = JsonParser.parseString(Files.readString(source.path())).getAsJsonObject();
        if (!carry.has("phase") || !carry.get("phase").getAsString().equals("complete"))
          throw new IOException("Place or recover the carried container in the original world before duplicating it. Its recovery data was preserved.");
      } catch (RuntimeException damaged) { throw new IOException("Carried-container recovery data needs repair before this world can be duplicated. A raw backup remains available.", damaged); }
    }
  }

  private static byte[] writeZip(Path zip, List<WorldSnapshotFiles.Source> sources, Map<String, WorldSnapshotFiles.Proof> proofs,
      WorldOwnedData.Owner owner, WorldOwnedData.Discovery external, Path game, BooleanSupplier cancelled, Progress progress, Faults faults) throws IOException {
    long done = 0, total = total(sources);
    byte[] manifest;
    try (ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zip, StandardOpenOption.CREATE_NEW)))) {
      output.setLevel(3);
      for (var source : sources) {
        WorldSnapshotFiles.check(cancelled); faults.at("copy-file", source.path());
        output.putNextEntry(new ZipEntry(source.entry()));
        if (!source.stamp().directory()) {
          proofs.put(source.entry(), WorldSnapshotFiles.transfer(source, output, cancelled)); done += source.stamp().size();
        }
        output.closeEntry(); progress.set("Writing backup", done, total);
      }
      output.putNextEntry(new ZipEntry(MANIFEST));
      manifest = manifest(owner, external, game, sources, proofs).toString().getBytes(StandardCharsets.UTF_8);
      output.write(manifest); output.closeEntry();
    }
    WorldSnapshotFiles.force(zip);
    return manifest;
  }

  private static void copyWorld(Path copy, Path maps, List<WorldSnapshotFiles.Source> sources,
      Map<String, WorldSnapshotFiles.Proof> proofs, BooleanSupplier cancelled, Progress progress, Faults faults) throws IOException {
    long done = 0, total = total(sources);
    for (var source : sources) {
      WorldSnapshotFiles.check(cancelled); faults.at("copy-file", source.path());
      Path target = null;
      if (source.kind().equals("world")) target = copy.resolve(source.entry().substring("world/".length()));
      else if (source.kind().equals("maps")) {
        String[] parts = source.entry().split("/", 5);
        target = parts.length == 5 ? maps.resolve(parts[4]) : maps;
      }
      if (source.stamp().directory()) {
        if (target != null) { SavedWorldAccess.safe(target); Files.createDirectories(target); }
      } else {
        proofs.put(source.entry(), target == null ? WorldSnapshotFiles.hash(source.path(), cancelled) : WorldSnapshotFiles.write(source, target, cancelled));
        done += source.stamp().size();
      }
      progress.set("Copying world", done, total);
    }
  }

  private static void rewriteMetadata(Path copy, String id, String name, BooleanSupplier cancelled) throws IOException {
    List<Path> metadata;
    try (var paths = Files.walk(copy)) {
      metadata = paths.filter(Files::isRegularFile).filter(p -> Set.of("level.dat", "level.dat_old", "level.dat_new").contains(p.getFileName().toString())).toList();
    }
    if (metadata.isEmpty()) throw new IOException("World metadata is missing.");
    if (metadata.size() > 256) throw new IOException("Too many metadata files for a safe world copy.");
    for (Path path : metadata) {
      WorldSnapshotFiles.check(cancelled);
      NbtCompound root = WorldRuleFiles.read(path), data = root.getCompound("Data");
      // Frozen legacy evidence belongs to the original ID. Resolve it before assigning the new one.
      var rules = WorldRuleData.read(data); WorldRuleData.write(data, rules);
      data.putString("PowerBetaCommandWorldId", id); data.putString("LevelName", name);
      byte[] fenced = WorldRuleFiles.fencedMetadata(root);
      FileTransaction.atomicWrite(path, fenced);
      NbtCompound saved = WorldRuleFiles.read(path).getCompound("Data");
      if (!saved.getString("PowerBetaCommandWorldId").equals(id) || !saved.getString("LevelName").equals(name))
        throw new IOException("Copied world metadata could not be verified.");
    }
    Set<Path> roots = new LinkedHashSet<>(); roots.add(copy);
    metadata.forEach(path -> roots.add(path.getParent()));
    for (Path root : roots) {
      Path level = root.resolve("level.dat"), old = root.resolve("level.dat_old");
      if (!Files.exists(level)) {
        byte[] fallback = SavedWorldAccess.metadataBytes(old);
        if (fallback == null) throw new IOException("World metadata is missing its primary and previous copies.");
        FileTransaction.atomicWrite(level, fallback);
      }
      if (!Files.exists(old)) FileTransaction.atomicWrite(old, SavedWorldAccess.metadataBytes(level));
    }
  }

  private static void validatePlayers(Path copy, BooleanSupplier cancelled) throws IOException {
    try (var paths = Files.walk(copy, 32)) {
      List<Path> directories = paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().equals("players")).limit(257).toList();
      if (directories.size() > 256) throw new IOException("Too many saved-player directories for a safe world copy.");
      for (Path players : directories) { WorldSnapshotFiles.check(cancelled); WorldRuleFiles.validatePlayers(players.getParent()); }
    }
  }

  private static void validateSources(SavedWorldAccess access, SavedWorldAccess.Lease lease, WorldOwnedData.Owner owner,
      Action action, BackupOptions options,
      List<WorldSnapshotFiles.Source> expected, Map<String, WorldSnapshotFiles.Proof> proofs,
      BooleanSupplier cancelled, Progress progress) throws IOException {
    access.checkIdle(lease);
    var current = WorldSnapshotFiles.collect(access.game(), access.world(), discover(access.game(), owner, action, options), cancelled);
    if (!expected.equals(current)) throw new IOException("World files changed. Reopen the saved-world menu and retry; the original was preserved.");
    int checked = 0;
    for (var source : expected) {
      WorldSnapshotFiles.check(cancelled);
      if (!source.stamp().directory() && !Objects.equals(proofs.get(source.entry()), WorldSnapshotFiles.hash(source.path(), cancelled)))
        throw new IOException("World contents changed during verification. The original was preserved.");
      progress.set("Verifying original files", ++checked, expected.size());
    }
    access.checkIdle(lease);
  }

  private static JsonObject manifest(WorldOwnedData.Owner owner, WorldOwnedData.Discovery external, Path game, List<WorldSnapshotFiles.Source> sources,
      Map<String, WorldSnapshotFiles.Proof> proofs) {
    JsonObject root = new JsonObject(); root.addProperty("version", 1); root.addProperty("name", owner.name());
    root.addProperty("created", System.currentTimeMillis());
    JsonArray ids = new JsonArray(); owner.ids().forEach(ids::add); root.add("worldIds", ids);
    JsonArray notes = new JsonArray(); external.warnings().forEach(notes::add); root.add("warnings", notes);
    JsonArray owned = new JsonArray();
    for (var entry : external.files()) {
      JsonObject file = new JsonObject(); file.addProperty("path", "external/" + game.relativize(entry.path()).toString().replace(File.separatorChar, '/'));
      file.addProperty("kind", entry.kind()); file.addProperty("uncertainOwnership", entry.uncertain()); file.addProperty("pendingRecovery", entry.pending());
      file.addProperty("duplicatePolicy", entry.kind().equals("maps") ? "copy-under-new-world-id" : "retain-with-original"); owned.add(file);
    }
    root.add("externalData", owned);
    JsonArray files = new JsonArray();
    for (var source : sources) {
      JsonObject file = new JsonObject(); file.addProperty("path", source.entry()); file.addProperty("kind", source.kind());
      file.addProperty("directory", source.stamp().directory());
      if (!source.stamp().directory()) { var proof = proofs.get(source.entry()); file.addProperty("bytes", proof.bytes()); file.addProperty("sha256", proof.sha256()); }
      files.add(file);
    }
    root.add("files", files); return root;
  }

  private static void verifyZip(Path zip, List<WorldSnapshotFiles.Source> sources, Map<String, WorldSnapshotFiles.Proof> proofs,
      byte[] manifest, BooleanSupplier cancelled) throws IOException {
    try (ZipFile archive = new ZipFile(zip.toFile())) {
      if (archive.size() != sources.size() + 1 || archive.getEntry(MANIFEST) == null) throw new IOException("Backup directory could not be verified.");
      ZipEntry manifestEntry = archive.getEntry(MANIFEST);
      if (manifestEntry.getSize() != manifest.length) throw new IOException("Backup manifest size could not be verified.");
      try (InputStream input = archive.getInputStream(manifestEntry)) {
        byte[] actual = input.readNBytes(manifest.length + 1); CRC32 crc = new CRC32(); crc.update(actual);
        if (!Arrays.equals(manifest, actual) || crc.getValue() != manifestEntry.getCrc()) throw new IOException("Backup manifest could not be verified.");
      }
      for (var source : sources) {
        WorldSnapshotFiles.check(cancelled); ZipEntry entry = archive.getEntry(source.entry());
        if (entry == null || entry.isDirectory() != source.stamp().directory()) throw new IOException("Backup entry is missing or invalid.");
        if (entry.isDirectory()) continue;
        var expected = proofs.get(source.entry());
        MessageDigest hash;
        try { hash = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        CRC32 crc = new CRC32(); long count = 0; byte[] buffer = new byte[WorldSnapshotFiles.BUFFER];
        try (InputStream input = archive.getInputStream(entry)) {
          for (int read; (read = input.read(buffer)) >= 0;) {
            WorldSnapshotFiles.check(cancelled); if (read == 0) continue;
            count += read; if (count > expected.bytes()) throw new IOException("Backup entry size changed.");
            hash.update(buffer, 0, read); crc.update(buffer, 0, read);
          }
        }
        if (count != expected.bytes() || crc.getValue() != entry.getCrc() || !HexFormat.of().formatHex(hash.digest()).equals(expected.sha256()))
          throw new IOException("Backup contents could not be verified.");
      }
    }
  }

  private static Map<Path, WorldSnapshotFiles.Proof> payload(Path copy, Path maps, BooleanSupplier cancelled) throws IOException {
    Map<Path, WorldSnapshotFiles.Proof> result = new LinkedHashMap<>();
    for (Path root : List.of(copy, maps)) {
      if (!Files.exists(root)) continue;
      try (var files = Files.walk(root)) {
        for (Path path : files.sorted().toList()) {
          WorldSnapshotFiles.check(cancelled); SavedWorldAccess.safe(path);
          if (Files.isRegularFile(path) && !path.equals(copy.resolve("power-beta-copy.json")) && !path.equals(copy.resolve(SavedWorldAccess.INCOMPLETE)))
            result.put(path, WorldSnapshotFiles.hash(path, cancelled));
        }
      }
    }
    return result;
  }
  private static void syncTree(Path root, BooleanSupplier cancelled) throws IOException {
    try (var files = Files.walk(root)) {
      for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
        WorldSnapshotFiles.check(cancelled);
        if (Files.isDirectory(path)) DurableFiles.syncDirectory(path); else WorldSnapshotFiles.force(path);
      }
    }
  }
  private static byte[] marker(String id, String name) {
    JsonObject marker = new JsonObject(); marker.addProperty("version", 1); marker.addProperty("worldId", id); marker.addProperty("name", name);
    return marker.toString().getBytes(StandardCharsets.UTF_8);
  }
  private static long total(List<WorldSnapshotFiles.Source> sources) throws IOException {
    long bytes = 0;
    try { for (var source : sources) if (!source.stamp().directory()) bytes = Math.addExact(bytes, source.stamp().size()); }
    catch (ArithmeticException tooLarge) { throw new IOException("World snapshot exceeds the supported size.", tooLarge); }
    return bytes;
  }
  private static String fileName(String name) {
    String file = name.replaceAll("[\\\\/:*?\"<>|]", "_").strip().replaceAll("[. ]+$", "");
    if (file.isEmpty()) file = "World";
    if (file.codePointCount(0, file.length()) > 48) file = file.substring(0, file.offsetByCodePoints(0, 48));
    if (file.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) file = "World_" + file;
    return file;
  }
  private static Path available(Path parent, String name, String suffix) throws IOException {
    SavedWorldAccess.safe(parent);
    for (int i = 0; i < 16; i++) {
      Path path = parent.resolve(name + "-" + UUID.randomUUID().toString() + suffix);
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return path;
    }
    throw new IOException("A unique destination name could not be reserved.");
  }
  private static void rename(Path stage, Path target) throws IOException {
    SavedWorldAccess.safe(stage); SavedWorldAccess.safe(target);
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("The destination already exists; nothing was replaced.");
    try { Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException unsupported) { throw new IOException("This filesystem cannot publish the verified copy atomically. The original world was preserved.", unsupported); }
  }
  private static void removeOwnedTree(Path tree) throws IOException {
    SavedWorldAccess.safe(tree);
    try (var files = Files.walk(tree)) { for (Path path : files.sorted(Comparator.reverseOrder()).toList()) { SavedWorldAccess.safe(path); Files.delete(path); } }
    DurableFiles.syncDirectory(tree.getParent());
  }
  private static void removeStage(Path root, Path stage) throws IOException {
    if (!root.equals(stage.getParent()) || !stage.getFileName().toString().startsWith("staging-")) throw new IOException("Invalid world staging directory.");
    removeOwnedTree(stage);
  }
}
