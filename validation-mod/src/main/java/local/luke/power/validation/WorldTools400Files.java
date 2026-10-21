package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import local.luke.power.config.FileTransaction;
import local.luke.power.offhand.OffhandData;
import local.luke.power.rules.*;
import local.luke.power.storage.AppendJournal;
import local.luke.power.world.*;
import net.minecraft.nbt.*;

/** Creates and deletes only token-marked runtime-fixture data, never an existing save. */
final class WorldTools400Files {
  static final String MARKER = ".world-tools-400-owner";
  final String token = UUID.randomUUID().toString(), id = UUID.randomUUID().toString();
  final String folder = "world-tools-400-" + token.substring(0, 12);
  final Path game, source, report;
  final RuleSet rules = new RuleSet();
  final Map<Path, byte[]> originals = new LinkedHashMap<>(), journals = new LinkedHashMap<>();
  final List<Path> worlds = new ArrayList<>(), maps = new ArrayList<>();
  final Map<Path, String> backups = new LinkedHashMap<>();
  final Set<Path> initialJournals;
  Path fresh;
  String freshIdentity;
  Set<Path> mapsBeforeCreate = Set.of();

  WorldTools400Files(Path game) throws Exception {
    this.game = game.toRealPath(); source = this.game.resolve("saves").resolve(folder);
    report = this.game.resolve("power-beta-data/reports/world-tools-400").resolve(token);
    check(!Files.exists(source, LinkOption.NOFOLLOW_LINKS), "fixture folder already exists");
    safe(source); Files.createDirectories(source.getParent()); Files.createDirectory(source); marker(source); worlds.add(source);
    Files.createDirectories(report); Files.writeString(report.resolve(MARKER), token, StandardOpenOption.CREATE_NEW);
    rules.set(GameRule.FIRE_SPREAD, false); rules.set(GameRule.ADVANCE_TIME, false);
    rules.set(GameRule.ALLOW_CREATIVE_MODE, false); rules.set(GameRule.ALLOW_SPECTATOR_MODE, false);
    writeMetadata(source.resolve("level.dat"), "primary"); writeMetadata(source.resolve("level.dat_old"), "fallback");
    writeMetadata(source.resolve("DIM-1/level.dat"), "nether");
    Files.createDirectories(source.resolve("players")); writeNbt(source.resolve("players/WorldTools400.dat"), player());
    Files.createDirectories(source.resolve("region"));
    byte[] payload = new byte[8 * 1024 * 1024]; new Random(400).nextBytes(payload);
    Files.write(source.resolve("region/r.0.0.mcr"), payload, StandardOpenOption.CREATE_NEW);
    Files.createDirectories(source.resolve("empty")); Files.createDirectories(source.resolve("data"));
    Files.writeString(source.resolve("data/fixture.txt"), "Synthetic fixture data " + token, StandardOpenOption.CREATE_NEW);
    Path map = map(id); safe(map); check(!Files.exists(map), "fixture map identity already exists");
    Files.createDirectories(map); marker(map); maps.add(map);
    Files.writeString(map.resolve("waypoints.json"), "{\"fixture\":\"" + token + "\",\"waypoints\":[]}", StandardOpenOption.CREATE_NEW);
    NbtCompound hotbar = new NbtCompound(); hotbar.putString("WorldId", id); hotbar.putInt("OwnershipVersion", 2); hotbar.putString("Player", "WorldTools400");
    Path hotbarPath = journalPath("hotbar-backups"); new AppendJournal(hotbarPath).append(nbt(hotbar)); journals.put(hotbarPath, Files.readAllBytes(hotbarPath));
    Path inventory = journalPath("inventory-recovery");
    new AppendJournal(inventory).append(("{\"format\":1,\"phase\":\"prepared\",\"world\":\"" + id + "\",\"player\":\"WorldTools400\"}").getBytes(StandardCharsets.UTF_8));
    journals.put(inventory, Files.readAllBytes(inventory));
    Path death = journalPath("deaths"); byte[] event = ("{\"kind\":\"death\",\"death\":{\"id\":\"fixture\",\"world\":\"" + id + "\"}}").getBytes(StandardCharsets.UTF_8);
    Files.createDirectories(death.getParent()); CRC32 crc = new CRC32(); crc.update(event);
    try (var output = new DataOutputStream(Files.newOutputStream(death, StandardOpenOption.CREATE_NEW))) {
      output.writeInt(0x50424448); output.writeInt(2); output.writeInt(event.length); output.writeLong(crc.getValue()); output.write(event);
    }
    journals.put(death, Files.readAllBytes(death));
    try (var paths = Files.walk(source)) { for (Path path : paths.filter(Files::isRegularFile).toList()) originals.put(source.relativize(path), Files.readAllBytes(path)); }
    initialJournals = journalFiles(); manifest();
  }
  private Path journalPath(String kind) { return game.resolve("power-beta-data").resolve(kind).resolve(folder + ".journal"); }
  private NbtCompound player() throws Exception {
    NbtCompound player = new NbtCompound(); player.putString("WorldTools400Player", token); player.putInt("Dimension", 0);
    NbtList items = new NbtList(); var item = DeathChecks.tagged(35, 12, 5, "world-tools-native");
    NbtCompound stack = item.writeNbt(new NbtCompound()); stack.putByte("Slot", (byte)9); items.add(stack); player.put("Inventory", items);
    OffhandData.write(player, DeathChecks.tagged(50, 117, 0, "world-tools-offhand"), null); return player;
  }
  private void writeMetadata(Path path, String variant) throws Exception {
    var properties = new net.minecraft.world.WorldProperties(0, folder); NbtCompound data = properties.asNbt();
    data.putLong("RandomSeed", 0); data.putLong("Time", 123456); data.putString("LevelName", folder);
    data.putInt("version", 19132); data.putString("PowerBetaCommandWorldId", id); data.putInt(WorldDifficulty.TAG, 3);
    data.putString("WorldTools400Variant", variant); data.put("Player", player()); WorldRuleData.write(data, rules);
    NbtCompound root = new NbtCompound(); root.put("Data", data); writeNbt(path, root);
  }
  static void writeNbt(Path path, NbtCompound root) throws IOException {
    Files.createDirectories(path.getParent()); ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.writeCompressed(root, bytes);
    FileTransaction.atomicWrite(path, bytes.toByteArray());
  }
  static byte[] nbt(NbtCompound root) throws IOException { ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.write(root, new DataOutputStream(bytes)); return bytes.toByteArray(); }
  Path map(String identity) { return game.resolve("power-beta-data/maps").resolve(UUID.nameUUIDFromBytes(("world:" + identity).getBytes(StandardCharsets.UTF_8)).toString()); }
  void unchanged() throws Exception {
    sourceUnchanged();
    check(initialJournals.equals(journalFiles()), "copy created or removed a recovery journal namespace");
  }
  void sourceUnchanged() throws Exception {
    try (var paths = Files.walk(source)) { check(paths.filter(Files::isRegularFile).count() == originals.size(), "fixture source file set changed"); }
    for (var entry : originals.entrySet()) check(Arrays.equals(entry.getValue(), Files.readAllBytes(source.resolve(entry.getKey()))), "fixture source changed: " + entry.getKey());
    for (var entry : journals.entrySet()) check(Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())), "recovery evidence changed: " + entry.getKey());
  }
  void backup(WorldSnapshots.Result result) throws Exception {
    Path zipPath = result.path().toAbsolutePath().normalize(); check(zipPath.getParent().equals(game.resolve("backups/power-beta")), "unexpected backup directory");
    try (ZipFile zip = new ZipFile(zipPath.toFile())) {
      check(token.equals(new String(zip.getInputStream(zip.getEntry("world/" + MARKER)).readAllBytes(), StandardCharsets.UTF_8)), "backup ownership marker missing");
      for (var entry : originals.entrySet()) check(Arrays.equals(entry.getValue(), zip.getInputStream(zip.getEntry("world/" + entry.getKey().toString().replace('\\', '/'))).readAllBytes()), "backup source bytes changed: " + entry.getKey());
      for (var entry : journals.entrySet()) check(Arrays.equals(entry.getValue(), zip.getInputStream(zip.getEntry("external/" + game.relativize(entry.getKey()).toString().replace('\\', '/'))).readAllBytes()), "backup journal bytes changed");
      JsonObject manifest = JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("power-beta-backup.json")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
      check(manifest.getAsJsonArray("worldIds").size() == 1 && manifest.getAsJsonArray("worldIds").get(0).getAsString().equals(id), "backup world identity missing");
      check(zip.getEntry("world/empty/") != null, "empty source directory missing");
    }
    backups.put(zipPath, sha(zipPath)); manifest(); unchanged();
  }
  String duplicate(WorldSnapshots.Result result) throws Exception {
    Path copy = result.path().toAbsolutePath().normalize(); ownedWorld(copy); worlds.add(copy); manifest();
    String identity = WorldRuleFiles.read(copy.resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    check(!identity.equals(id) && UUID.fromString(identity).toString().equals(identity), "copy reused original identity");
    Path copiedMap = map(identity); owned(copiedMap); maps.add(copiedMap); manifest();
    for (String file : List.of("level.dat", "level.dat_old", "DIM-1/level.dat", "DIM-1/level.dat_old")) {
      NbtCompound data = WorldRuleFiles.read(copy.resolve(file)).getCompound("Data");
      check(data.getString("PowerBetaCommandWorldId").equals(identity), "copy fallback/dimension identity differs");
      check(data.getLong("RandomSeed") == 0 && data.getLong("Time") == 123456 && data.getInt(WorldDifficulty.TAG) == 3, "copy changed seed/time/difficulty");
      check(data.getCompound(WorldRuleData.TAG).getInt("version") == RuleSet.VERSION && WorldRuleData.read(data).equals(rules), "copy rule fence/values differ");
      comparePlayer(file, data.getCompound("Player"), identity);
    }
    for (var entry : originals.entrySet()) if (!entry.getKey().getFileName().toString().startsWith("level.dat"))
      check(Arrays.equals(entry.getValue(), Files.readAllBytes(copy.resolve(entry.getKey()))), "copy payload changed: " + entry.getKey());
    check(!Files.exists(copy.resolve(SavedWorldAccess.INCOMPLETE)), "published copy remains incomplete");
    check(Arrays.equals(Files.readAllBytes(map(id).resolve("waypoints.json")), Files.readAllBytes(copiedMap.resolve("waypoints.json"))), "map payload changed");
    String namespace = UUID.nameUUIDFromBytes((identity + "\nWorldTools400").getBytes(StandardCharsets.UTF_8)).toString();
    check(!Files.exists(game.resolve("power-beta-data/hotbar-backups/world-" + namespace + ".journal"))
        && !Files.exists(game.resolve("power-beta-data/inventory-recovery/" + namespace + ".journal")), "copy shares recovery journal namespace");
    SavedWorldAccess.requireOpenable(game, copy.getFileName().toString()); manifest(); unchanged(); return identity;
  }
  private void comparePlayer(String file, NbtCompound copied, String identity) throws Exception {
    Path relative = Path.of(file); byte[] metadata = originals.get(relative);
    // A missing previous file is created from that dimension's persisted primary metadata.
    if (metadata == null && relative.getFileName().toString().equals("level.dat_old"))
      metadata = originals.get(relative.resolveSibling("level.dat"));
    check(metadata != null, "fixture has no persisted source metadata for " + file);
    NbtCompound original = NbtIo.readCompressed(new ByteArrayInputStream(metadata)).getCompound("Data").getCompound("Player");
    var hand = OffhandData.read(original);
    check(original.getString("WorldTools400Player").equals(token) && hand != null && hand.count == 117,
        "persisted source player/offhand fixture differs for " + file);
    // put("Player", value) names the compound. Compare both persisted embedded roots,
    // including that native framing, instead of a newly constructed unnamed root.
    byte[] before = nbt(original), after = nbt(copied);
    Path evidence = report.resolve("duplicate-" + identity); Files.createDirectories(evidence);
    String name = file.replace('/', '_');
    Files.write(evidence.resolve(name + "-source-player.nbt"), before, StandardOpenOption.CREATE_NEW);
    Files.write(evidence.resolve(name + "-copy-player.nbt"), after, StandardOpenOption.CREATE_NEW);
    check(Arrays.equals(before, after), "copy changed full native player/offhand data in " + file
        + "; source root=" + original.getKey() + " bytes=" + before.length
        + ", copy root=" + copied.getKey() + " bytes=" + after.length + "; retained " + evidence);
  }
  void rememberBackup(Path path) throws Exception {
    check(path.toAbsolutePath().normalize().getParent().equals(game.resolve("backups/power-beta")), "unexpected archive directory");
    try (ZipFile zip = new ZipFile(path.toFile())) {
      check(token.equals(new String(zip.getInputStream(zip.getEntry("world/" + MARKER)).readAllBytes(), StandardCharsets.UTF_8)), "archive ownership marker missing");
    }
    backups.put(path.toAbsolutePath().normalize(), sha(path)); manifest();
  }
  void created(Path path) throws Exception { check(path.equals(fresh), "native creation used an unexpected directory"); marker(path); worlds.add(path); manifest(); }
  void beforeCreate() throws IOException {
    Path root = game.resolve("power-beta-data/maps");
    if (!Files.exists(root)) { mapsBeforeCreate = Set.of(); return; }
    try (var paths = Files.list(root)) { mapsBeforeCreate = Set.copyOf(paths.toList()); }
  }
  void freshMap() throws Exception {
    if (freshIdentity == null) return;
    Path path = map(freshIdentity); if (!Files.exists(path) || maps.contains(path)) return;
    check(!mapsBeforeCreate.contains(path), "fresh world reused pre-existing map data; retained");
    safe(path); marker(path); maps.add(path); manifest();
  }
  void manifest() throws IOException {
    JsonObject root = new JsonObject(); root.addProperty("format", 1); root.addProperty("token", token); root.addProperty("sourceWorldId", id);
    root.addProperty("source", game.relativize(source).toString()); if (fresh != null) root.addProperty("fresh", game.relativize(fresh).toString());
    JsonArray paths = new JsonArray(); for (Path path : worlds) paths.add(game.relativize(path).toString()); root.add("worlds", paths);
    paths = new JsonArray(); for (Path path : maps) paths.add(game.relativize(path).toString()); root.add("maps", paths);
    JsonObject archives = new JsonObject(); backups.forEach((path, hash) -> archives.addProperty(game.relativize(path).toString(), hash)); root.add("backups", archives);
    paths = new JsonArray(); for (Path path : journals.keySet()) paths.add(game.relativize(path).toString()); root.add("journals", paths);
    FileTransaction.atomicWrite(report.resolve("ownership.json"), root.toString().getBytes(StandardCharsets.UTF_8));
  }
  void cleanup() throws Exception {
    for (Path path : worlds) ownedWorld(path);
    for (Path path : maps) owned(path);
    for (var entry : backups.entrySet()) check(entry.getKey().getParent().equals(game.resolve("backups/power-beta")) && entry.getValue().equals(sha(entry.getKey())), "backup ownership changed; retained");
    for (var entry : journals.entrySet()) check(Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())), "journal ownership changed; retained");
    for (Path path : worlds) remove(path); for (Path path : maps) remove(path);
    for (Path path : backups.keySet()) Files.delete(path); for (Path path : journals.keySet()) Files.delete(path);
    Files.writeString(report.resolve("cleanup-complete.txt"), "Removed only token-marked fixture worlds/maps and byte-verified fixture archives/journals.\n", StandardOpenOption.CREATE_NEW);
  }
  private void ownedWorld(Path path) throws IOException { check(path.getParent().equals(game.resolve("saves")) && path.getFileName().toString().startsWith(folder), "refusing a nonfixture world path"); owned(path); }
  private void owned(Path path) throws IOException { safe(path); check(Files.readString(path.resolve(MARKER)).equals(token), "fixture ownership marker differs: " + path); }
  private void marker(Path path) throws IOException { FileTransaction.atomicWrite(path.resolve(MARKER), token.getBytes(StandardCharsets.UTF_8)); }
  private void remove(Path root) throws IOException {
    owned(root); List<Path> paths;
    try (var walk = Files.walk(root)) { paths = walk.sorted(Comparator.reverseOrder()).toList(); }
    for (Path path : paths) safe(path); for (Path path : paths) Files.delete(path);
  }
  private static void safe(Path path) throws IOException { for (Path part = path; part != null; part = part.getParent()) if (Files.isSymbolicLink(part)) throw new IOException("Fixture refuses a symlink: " + part); }
  private Set<Path> journalFiles() throws IOException {
    Set<Path> result = new HashSet<>(); for (String kind : List.of("deaths", "hotbar-backups", "inventory-recovery")) {
      Path root = game.resolve("power-beta-data").resolve(kind); if (!Files.exists(root)) continue;
      try (var paths = Files.list(root)) { result.addAll(paths.filter(Files::isRegularFile).toList()); }
    } return result;
  }
  private static String sha(Path path) throws Exception { MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[65536]; try (InputStream in = Files.newInputStream(path)) { for (int n; (n = in.read(buffer)) >= 0;) if (n > 0) digest.update(buffer, 0, n); } return HexFormat.of().formatHex(digest.digest()); }
}
