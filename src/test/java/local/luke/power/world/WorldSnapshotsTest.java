package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorldSnapshotsTest {
  @TempDir Path game;
  private Path world;
  private Path previousConfig;
  private final String id = "a7b54c59-e442-44eb-b2e8-39ca30d101b5";
  private static final WorldSnapshots.Progress QUIET = (phase, done, total) -> {};
  private static final WorldSnapshots.Faults NORMAL = (phase, path) -> {};

  @BeforeEach void setup() throws Exception {
    PowerConfig.flushPending();
    var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true);
    previousConfig = (Path)field.get(null); field.set(null, null); PowerConfig.configure(game.resolve("config/power-beta.json"));
    world = game.resolve("saves/Original"); Files.createDirectories(world.resolve("region"));
    writeMetadata(world.resolve("level.dat"), id, "Original", 0);
    Files.copy(world.resolve("level.dat"), world.resolve("level.dat_old"));
    Files.writeString(world.resolve("region/r.0.0.mcr"), "native region bytes");
    Files.createDirectories(world.resolve("empty"));
  }
  @AfterEach void restoreConfig() throws Exception {
    PowerConfig.flushPending(); var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true); field.set(null, previousConfig);
  }
  private void writeMetadata(Path path, String identity, String name, long seed) throws Exception {
    Files.createDirectories(path.getParent());
    NbtCompound root = new NbtCompound(), data = new NbtCompound(), player = new NbtCompound();
    data.putString("LevelName", name); data.putLong("RandomSeed", seed); data.putLong("Time", 123456);
    data.putString("PowerBetaCommandWorldId", identity); data.putInt(WorldDifficulty.TAG, 3);
    player.putString("NativeInventory", "untouched"); data.put("Player", player);
    RuleSet rules = new RuleSet(); rules.set(GameRule.FIRE_SPREAD, false); WorldRuleData.write(data, rules); root.put("Data", data);
    try (OutputStream output = Files.newOutputStream(path)) { NbtIo.writeCompressed(root, output); }
  }
  private SavedWorldAccess access() throws Exception { return new SavedWorldAccess(game, "Original", () -> true); }
  private WorldSnapshots.Result run(WorldSnapshots.Action action) throws Exception {
    return WorldSnapshots.execute(access(), action, "A copy", () -> false, QUIET, NORMAL);
  }
  private Path maps(String identity) {
    return game.resolve("power-beta-data/maps/" + UUID.nameUUIDFromBytes(("world:" + identity).getBytes(StandardCharsets.UTF_8)));
  }
  private Map<Path, byte[]> originals() throws Exception {
    Map<Path, byte[]> bytes = new HashMap<>();
    try (var paths = Files.walk(world)) { for (Path path : paths.filter(Files::isRegularFile).toList()) bytes.put(path, Files.readAllBytes(path)); }
    return bytes;
  }
  private void unchanged(Map<Path, byte[]> expected) throws Exception {
    for (var entry : expected.entrySet()) assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), entry.getKey().toString());
    try (var paths = Files.walk(world)) { assertEquals(expected.size(), paths.filter(Files::isRegularFile).count()); }
  }
  private void noStage() throws Exception {
    try (var paths = Files.list(game.resolve("power-beta-data/world-operations"))) {
      assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith("staging-")));
    }
  }
  private void journal(Path path, int magic, byte[] payload) throws Exception {
    Files.createDirectories(path.getParent()); CRC32 crc = new CRC32(); crc.update(payload);
    try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
      output.writeInt(magic); output.writeInt(magic == 0x50424448 ? 2 : 1); output.writeInt(payload.length); output.writeLong(crc.getValue()); output.write(payload);
    }
  }
  private void death(Path path, String identity, boolean pending) throws Exception {
    journal(path, 0x50424448, ("{\"kind\":\"death\",\"death\":{\"id\":\"1\",\"world\":\"" + identity + "\"}}").getBytes(StandardCharsets.UTF_8));
    if (pending) journal(path, 0x50424448, "{\"kind\":\"pending\",\"id\":\"1\"}".getBytes(StandardCharsets.UTF_8));
  }

  @Test void backupIncludesOriginalFallbackEmptyDirectoriesAndOwnedExternalData() throws Exception {
    var before = originals();
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("waypoints.json"), "map data");
    Path owned = game.resolve("power-beta-data/deaths/owned.journal"); death(owned, id, false);
    death(game.resolve("power-beta-data/deaths/unrelated.journal"), UUID.randomUUID().toString(), false);
    Path damaged = game.resolve("power-beta-data/hotbar-backups/old.journal"); Files.createDirectories(damaged.getParent()); Files.writeString(damaged, "retained evidence");
    var result = run(WorldSnapshots.Action.BACKUP);
    try (ZipFile zip = new ZipFile(result.path().toFile())) {
      for (var entry : before.entrySet()) assertArrayEquals(entry.getValue(), zip.getInputStream(zip.getEntry("world/" + world.relativize(entry.getKey()))).readAllBytes());
      assertNotNull(zip.getEntry("world/empty/"));
      assertNotNull(zip.getEntry("external/power-beta-data/maps/" + maps(id).getFileName() + "/waypoints.json"));
      assertNotNull(zip.getEntry("external/power-beta-data/deaths/owned.journal"));
      assertNull(zip.getEntry("external/power-beta-data/deaths/unrelated.journal"));
      assertNotNull(zip.getEntry("external/power-beta-data/hotbar-backups/old.journal"));
      JsonObject manifest = JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("power-beta-backup.json")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertEquals(id, manifest.getAsJsonArray("worldIds").get(0).getAsString());
      assertTrue(manifest.getAsJsonArray("files").size() > before.size());
    }
    assertFalse(result.warnings().isEmpty()); unchanged(before); noStage();
  }
  @Test void duplicateChangesEveryIdentityFencesEveryFallbackAndKeepsNativeData() throws Exception {
    writeMetadata(world.resolve("DIM-1/level.dat"), id, "Original", 0);
    var before = originals();
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("waypoints.json"), "map data");
    death(game.resolve("power-beta-data/deaths/owned.journal"), id, false);
    var result = run(WorldSnapshots.Action.DUPLICATE); Path copy = result.path();
    String newId = WorldRuleFiles.read(copy.resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertNotEquals(id, newId); assertEquals(newId, UUID.fromString(newId).toString());
    for (String path : List.of("level.dat", "level.dat_old", "DIM-1/level.dat", "DIM-1/level.dat_old")) {
      NbtCompound data = WorldRuleFiles.read(copy.resolve(path)).getCompound("Data");
      assertEquals(newId, data.getString("PowerBetaCommandWorldId")); assertEquals("A copy", data.getString("LevelName"));
      assertEquals(0, data.getLong("RandomSeed")); assertEquals(123456, data.getLong("Time"));
      assertEquals(RuleSet.VERSION, data.getCompound("GameRules").getInt("version"));
      assertFalse(WorldRuleData.read(data).enabled(GameRule.FIRE_SPREAD));
      assertEquals("untouched", data.getCompound("Player").getString("NativeInventory"));
    }
    assertEquals("native region bytes", Files.readString(copy.resolve("region/r.0.0.mcr")));
    assertEquals("map data", Files.readString(maps(newId).resolve("waypoints.json")));
    assertFalse(Files.exists(copy.resolve(SavedWorldAccess.INCOMPLETE)));
    try (var files = Files.list(game.resolve("power-beta-data/deaths"))) { assertEquals(1, files.count()); }
    JsonObject manifest = JsonParser.parseString(Files.readString(copy.resolve("power-beta-copy.json"))).getAsJsonObject();
    assertEquals(newId, manifest.get("duplicateWorldId").getAsString()); assertTrue(manifest.has("payload"));
    SavedWorldAccess.requireOpenable(game, copy.getFileName().toString()); unchanged(before); noStage();
  }

  @Test void backupIncludesConfigByDefaultWithoutChangingItsBytes() throws Exception {
    Path config = game.resolve("config/power-beta.json");
    Files.createDirectories(config.getParent());
    byte[] before = "{\"schemaVersion\":8,\"settings\":{},\"preserved\":true}\n".getBytes(StandardCharsets.UTF_8);
    Files.write(config, before);
    try (ZipFile zip = new ZipFile(run(WorldSnapshots.Action.BACKUP).path().toFile())) {
      assertArrayEquals(before, zip.getInputStream(zip.getEntry("external/config/power-beta.json")).readAllBytes());
    }
    assertArrayEquals(before, Files.readAllBytes(config));
    var copy = run(WorldSnapshots.Action.DUPLICATE).path();
    assertFalse(Files.exists(copy.resolve("config/power-beta.json")));
    assertArrayEquals(before, Files.readAllBytes(config));
  }

  @Test void optionalBackupFilesCanBeOmittedWhileRecoveryAndPendingMetadataStayIncluded() throws Exception {
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("waypoints.json"), "map data");
    Path config = game.resolve("config/power-beta.json"); Files.createDirectories(config.getParent()); Files.writeString(config, "{}");
    Path death = game.resolve("power-beta-data/deaths/owned.journal"); death(death, id, false);
    for (String state : List.of("complete", "pending")) {
      Path backup = game.resolve("power-beta-data/settings-backups/" + state); Files.createDirectories(backup);
      Files.writeString(backup.resolve(state + ".json"), "{\"saves/Original/level.dat\":\"file-0\"}");
      Files.writeString(backup.resolve("file-0"), "original metadata");
    }
    Path uncertain = game.resolve("power-beta-data/settings-backups/uncertain"); Files.createDirectories(uncertain);
    Files.writeString(uncertain.resolve("complete.json"), "unreadable evidence");
    var result = WorldSnapshots.execute(access(), WorldSnapshots.Action.BACKUP, "Minimal",
        new WorldSnapshots.BackupOptions(false, false, false), () -> false, QUIET, NORMAL);
    try (ZipFile zip = new ZipFile(result.path().toFile())) {
      assertNotNull(zip.getEntry("world/level.dat")); assertNotNull(zip.getEntry("world/region/r.0.0.mcr"));
      assertNotNull(zip.getEntry("external/power-beta-data/deaths/owned.journal"));
      assertNotNull(zip.getEntry("external/power-beta-data/settings-backups/pending/file-0"));
      assertNotNull(zip.getEntry("external/power-beta-data/settings-backups/uncertain/complete.json"));
      assertNull(zip.getEntry("external/config/power-beta.json"));
      assertNull(zip.getEntry("external/power-beta-data/maps/" + maps(id).getFileName() + "/waypoints.json"));
      assertNull(zip.getEntry("external/power-beta-data/settings-backups/complete/file-0"));
    }
    assertEquals("{}", Files.readString(config)); assertEquals("map data", Files.readString(maps(id).resolve("waypoints.json")));
  }

  @Test void enabledBackupConfigCannotChangeBeforePublication() throws Exception {
    Path config = game.resolve("config/power-beta.json"); Files.createDirectories(config.getParent()); Files.writeString(config, "{\"value\":1}");
    assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.BACKUP, "Changed config", () -> false, QUIET,
        (phase, path) -> { if (phase.equals("before-publish")) Files.writeString(config, "{\"value\":2}"); }));
    assertEquals("{\"value\":2}", Files.readString(config));
    try (var paths = Files.list(game.resolve("backups/power-beta"))) { assertEquals(0, paths.count()); }
    noStage();
  }
  @Test void successiveCopiesHaveIndependentIdsAndMapDirectories() throws Exception {
    String first = WorldRuleFiles.read(run(WorldSnapshots.Action.DUPLICATE).path().resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    String second = WorldRuleFiles.read(run(WorldSnapshots.Action.DUPLICATE).path().resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertNotEquals(first, second);
  }
  @Test void unopenedCopyOfCopyRetainsItsScopedSortingLayoutWithoutMutatingGlobalSettings() throws Exception {
    var settings = JsonParser.parseString("{\"inventoryTools\":{\"protectedSlots\":[31],\"preferredItems\":{}}}").getAsJsonObject();
    var data = local.luke.power.inventory.tools.WorldSortingLayoutsData.migrate(settings).seed(List.of(id));
    var active = data.layout(id); active.add("protectedSlots", JsonParser.parseString("[35]"));
    active.add("presets", JsonParser.parseString("{\"format\":999,\"future\":true}")); data = data.replace(id, active);
    settings.add(local.luke.power.inventory.tools.WorldSortingLayoutsData.SECTION, data.json());
    JsonObject config = new JsonObject(); config.addProperty("schemaVersion", 8); config.add("settings", settings);
    Path path = game.resolve("config/power-beta.json"); Files.createDirectories(path.getParent()); Files.writeString(path, config.toString()); byte[] original = Files.readAllBytes(path);
    Path first = run(WorldSnapshots.Action.DUPLICATE).path();
    var firstAccess = new SavedWorldAccess(game, first.getFileName().toString(), () -> true);
    Path second = WorldSnapshots.execute(firstAccess, WorldSnapshots.Action.DUPLICATE, "Second copy", () -> false, QUIET, NORMAL).path();
    String secondId = WorldRuleFiles.read(second.resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertEquals(active, local.luke.power.inventory.tools.WorldSortingLayoutCopies.imported(second, secondId));
    assertArrayEquals(original, Files.readAllBytes(path));
  }
  @Test void duplicateOpensAndBacksUpAfterOriginalWorldAndExternalDataAreRemoved() throws Exception {
    writeMetadata(world.resolve("DIM-1/level.dat"), id, "Original", 0);
    Files.createDirectories(world.resolve("data")); Files.writeString(world.resolve("data/map_7.dat"), "native map bytes");
    Files.createDirectories(world.resolve("players"));
    NbtCompound player = new NbtCompound(); player.putString("warps", "saved player warps");
    try (OutputStream out = Files.newOutputStream(world.resolve("players/Player.dat"))) { NbtIo.writeCompressed(player, out); }
    byte[] playerBytes = Files.readAllBytes(world.resolve("players/Player.dat"));
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("waypoints.json"), "saved waypoints");
    Files.writeString(maps(id).resolve("tiles"), "explored map tiles");
    Path deaths = game.resolve("power-beta-data/deaths/owned.journal"); death(deaths, id, false);
    Path inventory = game.resolve("power-beta-data/inventory-recovery/prepared.journal");
    journal(inventory, 0x4c504b48, ("{\"format\":1,\"phase\":\"prepared\",\"world\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8));
    Path settings = game.resolve("power-beta-data/settings-backups/source-save"); Files.createDirectories(settings);
    Files.writeString(settings.resolve("complete.json"), "{\"saves/Original/level.dat\":\"file-0\"}");
    Files.writeString(settings.resolve("file-0"), "original metadata evidence");

    Path copy = run(WorldSnapshots.Action.DUPLICATE).path();
    String copyId = WorldRuleFiles.read(copy.resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertFalse(Files.isSameFile(world.resolve("region/r.0.0.mcr"), copy.resolve("region/r.0.0.mcr")));
    assertFalse(Files.isSameFile(maps(id).resolve("tiles"), maps(copyId).resolve("tiles")));
    Files.writeString(world.resolve("region/r.0.0.mcr"), "later original terrain");
    Files.writeString(maps(id).resolve("tiles"), "later original exploration");
    assertEquals("native region bytes", Files.readString(copy.resolve("region/r.0.0.mcr")));
    assertEquals("explored map tiles", Files.readString(maps(copyId).resolve("tiles")));
    for (Path root : List.of(world, maps(id), deaths, inventory, settings)) {
      try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
    }

    // The provenance manifest still names the original ID. Opening and discovering
    // data must use only the duplicate's metadata and its own copied map directory.
    JsonObject provenance = JsonParser.parseString(Files.readString(copy.resolve("power-beta-copy.json"))).getAsJsonObject();
    assertEquals(id, provenance.getAsJsonArray("worldIds").get(0).getAsString());
    WorldRuleFiles.beforeOpen(game, copy.getFileName().toString());
    var owner = WorldOwnedData.owner(copy); assertEquals(Set.of(copyId), owner.ids());
    var external = WorldOwnedData.discover(game, owner);
    assertEquals(List.of(maps(copyId)), external.files().stream().map(WorldOwnedData.External::path).toList());
    for (String file : List.of("level.dat", "level.dat_old", "DIM-1/level.dat", "DIM-1/level.dat_old"))
      assertEquals(copyId, WorldRuleFiles.read(copy.resolve(file)).getCompound("Data").getString("PowerBetaCommandWorldId"));
    assertArrayEquals(playerBytes, Files.readAllBytes(copy.resolve("players/Player.dat")));
    var access = new SavedWorldAccess(game, copy.getFileName().toString(), () -> true);
    var backup = WorldSnapshots.execute(access, WorldSnapshots.Action.BACKUP, "Independent copy", () -> false, QUIET, NORMAL);
    try (ZipFile zip = new ZipFile(backup.path().toFile())) {
      assertEquals("native region bytes", new String(zip.getInputStream(zip.getEntry("world/region/r.0.0.mcr")).readAllBytes(), StandardCharsets.UTF_8));
      assertArrayEquals(playerBytes, zip.getInputStream(zip.getEntry("world/players/Player.dat")).readAllBytes());
      assertNotNull(zip.getEntry("world/data/map_7.dat"));
      assertNotNull(zip.getEntry("external/power-beta-data/maps/" + maps(copyId).getFileName() + "/waypoints.json"));
      assertNull(zip.getEntry("external/power-beta-data/deaths/owned.journal"));
      assertNull(zip.getEntry("external/power-beta-data/inventory-recovery/prepared.journal"));
    }
  }
  @Test void missingPrimaryIsCreatedFromVerifiedPreviousMetadataWithoutTouchingOriginal() throws Exception {
    Files.delete(world.resolve("level.dat")); var before = originals();
    var result = run(WorldSnapshots.Action.DUPLICATE);
    assertArrayEquals(Files.readAllBytes(result.path().resolve("level.dat")), Files.readAllBytes(result.path().resolve("level.dat_old")));
    unchanged(before);
  }
  @Test void conflictingDimensionIdentityBlocksDuplicateButAllowsRawBackup() throws Exception {
    writeMetadata(world.resolve("DIM-1/level.dat"), UUID.randomUUID().toString(), "Another", 1);
    var before = originals(); assertThrows(IOException.class, () -> run(WorldSnapshots.Action.DUPLICATE));
    assertTrue(Files.isRegularFile(run(WorldSnapshots.Action.BACKUP).path())); unchanged(before);
  }
  @Test void futureRuleFormatBlocksDuplicateButRemainsBackedUpByteExactly() throws Exception {
    NbtCompound root = WorldRuleFiles.read(world.resolve("level.dat")); root.getCompound("Data").getCompound("GameRules").putInt("version", 999);
    try (OutputStream out = Files.newOutputStream(world.resolve("level.dat"))) { NbtIo.writeCompressed(root, out); }
    var before = originals(); assertThrows(IllegalArgumentException.class, () -> run(WorldSnapshots.Action.DUPLICATE));
    assertTrue(Files.isRegularFile(run(WorldSnapshots.Action.BACKUP).path())); unchanged(before); noStage();
  }
  @Test void unsupportedDetachedPlayerDataBlocksDuplicateBeforePublicationButNotRawBackup() throws Exception {
    NbtCompound player = new NbtCompound(), offhand = new NbtCompound(); offhand.putInt("format", 999); player.put("PowerBetaOffhand", offhand);
    Path path = world.resolve("players/Player.dat"); Files.createDirectories(path.getParent());
    try (OutputStream out = Files.newOutputStream(path)) { NbtIo.writeCompressed(player, out); }
    var before = originals(); assertThrows(IOException.class, () -> run(WorldSnapshots.Action.DUPLICATE));
    assertTrue(Files.isRegularFile(run(WorldSnapshots.Action.BACKUP).path())); unchanged(before); noStage();
    try (var files = Files.list(game.resolve("saves"))) { assertEquals(1, files.count()); }
  }
  @Test void allActiveCarryPhasesBlockDuplicateAndRawBackupPreservesSoleContents() throws Exception {
    Path carry = world.resolve("data/power-beta-carry.dat"); Files.createDirectories(carry.getParent());
    for (String phase : List.of("removing", "held", "placing", "failed")) {
      Files.writeString(carry, "{\"phase\":\"" + phase + "\",\"onlyInventoryCopy\":\"retain\"}");
      var before = originals(); assertThrows(IOException.class, () -> run(WorldSnapshots.Action.DUPLICATE)); unchanged(before);
    }
    assertTrue(Files.isRegularFile(run(WorldSnapshots.Action.BACKUP).path()));
    Files.writeString(carry, "{\"phase\":\"complete\"}"); assertTrue(Files.isDirectory(run(WorldSnapshots.Action.DUPLICATE).path()));
  }
  @Test void pendingDeathClaimBlocksCopyWithoutReplayingOrChangingJournal() throws Exception {
    Path record = game.resolve("power-beta-data/deaths/pending.journal"); death(record, id, true); byte[] before = Files.readAllBytes(record);
    assertThrows(IOException.class, () -> run(WorldSnapshots.Action.DUPLICATE));
    assertTrue(Files.isRegularFile(run(WorldSnapshots.Action.BACKUP).path())); assertArrayEquals(before, Files.readAllBytes(record));
  }
  @Test void cancellationAtEveryPreparationBoundaryPublishesNothingAndReleasesLease() throws Exception {
    for (var action : WorldSnapshots.Action.values()) for (String boundary : List.of("collected", "copy-file", "verified-copy", "before-publish")) {
      AtomicBoolean cancel = new AtomicBoolean(); var before = originals();
      assertThrows(InterruptedIOException.class, () -> WorldSnapshots.execute(access(), action, "Cancel", cancel::get, QUIET,
          (phase, path) -> { if (phase.equals(boundary)) cancel.set(true); }));
      unchanged(before); noStage(); SavedWorldAccess.requireAvailable(game);
      try (var paths = Files.list(game.resolve("saves"))) { assertEquals(1, paths.count()); }
    }
  }
  @Test void ioFailureAtEveryPrepublicationBoundaryPreservesSourceAndCleansStage() throws Exception {
    for (var action : WorldSnapshots.Action.values()) for (String boundary : List.of("collected", "copy-file", "verified-copy", "before-publish")) {
      var before = originals();
      assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), action, "Fail", () -> false, QUIET,
          (phase, path) -> { if (phase.equals(boundary)) throw new IOException("Injected " + phase); }));
      unchanged(before); noStage(); SavedWorldAccess.requireAvailable(game);
    }
  }
  @Test void sourceHashChangeWithRestoredTimestampStillCancelsPublication() throws Exception {
    Path region = world.resolve("region/r.0.0.mcr"); var stamp = Files.getLastModifiedTime(region);
    assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.BACKUP, "Changed", () -> false, QUIET,
        (phase, path) -> { if (phase.equals("verified-copy")) { Files.writeString(region, "changed region!!!!!"); Files.setLastModifiedTime(region, stamp); } }));
    assertEquals("changed region!!!!!", Files.readString(region)); noStage();
  }
  @Test void damagedStagedZipNeverPublishes() throws Exception {
    var before = originals();
    assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.BACKUP, "Damaged", () -> false, QUIET,
        (phase, path) -> { if (phase.equals("copied")) Files.writeString(path, "broken archive", StandardOpenOption.TRUNCATE_EXISTING); }));
    unchanged(before); noStage();
  }
  @Test void failureAfterMapPublicationRemovesOnlyTheNewOrphanMap() throws Exception {
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("tiles"), "source tiles");
    for (String boundary : List.of("maps-renamed", "maps-published")) {
      assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.DUPLICATE, "Fail", () -> false, QUIET,
          (phase, path) -> { if (phase.equals(boundary)) throw new IOException("Injected map publication failure"); }));
      try (var paths = Files.list(maps(id).getParent())) { assertEquals(1, paths.count()); }
    }
    assertEquals("source tiles", Files.readString(maps(id).resolve("tiles")));
    try (var paths = Files.list(maps(id).getParent())) { assertEquals(1, paths.count()); } noStage();
  }
  @Test void failureAfterWorldRenameLeavesBlockedCopyWithRecoveryManifest() throws Exception {
    var before = originals();
    assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.DUPLICATE, "Fail", () -> false, QUIET,
        (phase, path) -> { if (phase.equals("published")) throw new IOException("Injected directory sync failure"); }));
    Path copy;
    try (var paths = Files.list(game.resolve("saves"))) { copy = paths.filter(path -> !path.equals(world)).findFirst().orElseThrow(); }
    assertTrue(Files.exists(copy.resolve("power-beta-copy.json"))); assertTrue(Files.exists(copy.resolve(SavedWorldAccess.INCOMPLETE)));
    assertThrows(IOException.class, () -> SavedWorldAccess.requireOpenable(game, copy.getFileName().toString())); unchanged(before); noStage();
  }
  @Test void directorySyncFailureImmediatelyAfterWorldRenameRetainsPublishedMapPayload() throws Exception {
    Files.createDirectories(maps(id)); Files.writeString(maps(id).resolve("tiles"), "source tiles");
    assertThrows(IOException.class, () -> WorldSnapshots.execute(access(), WorldSnapshots.Action.DUPLICATE, "Sync fail", () -> false, QUIET,
        (phase, path) -> { if (phase.equals("world-renamed")) throw new IOException("Injected parent sync failure"); }));
    Path copy;
    try (var paths = Files.list(game.resolve("saves"))) { copy = paths.filter(path -> !path.equals(world)).findFirst().orElseThrow(); }
    String newId = WorldRuleFiles.read(copy.resolve("level.dat")).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertEquals("source tiles", Files.readString(maps(newId).resolve("tiles")));
    assertTrue(Files.exists(copy.resolve(SavedWorldAccess.INCOMPLETE)));
    assertThrows(IOException.class, () -> SavedWorldAccess.requireOpenable(game, copy.getFileName().toString())); noStage();
  }
  @Test void symbolicLinkAnywhereInsideSourceBlocksBothActionsWithoutFollowingIt() throws Exception {
    Path outside = game.resolve("outside"); Files.writeString(outside, "private"); Files.createSymbolicLink(world.resolve("linked"), outside);
    for (var action : WorldSnapshots.Action.values()) assertThrows(IOException.class, () -> run(action));
    assertEquals("private", Files.readString(outside)); noStage();
  }
  @Test void ownedSettingsAndWorldMetadataBackupsAreIncludedButUnrelatedOnesAreNot() throws Exception {
    Path settings = game.resolve("power-beta-data/settings-backups/save-1"); Files.createDirectories(settings);
    Files.writeString(settings.resolve("complete.json"), "{\"saves/Original/level.dat\":\"file-0\"}"); Files.writeString(settings.resolve("file-0"), "evidence");
    Path metadata = game.resolve("power-beta-data/world-metadata-backups/upgrade-1"); Files.createDirectories(metadata);
    Files.writeString(metadata.resolve("transaction.json"), "{\"format\":1,\"state\":\"complete\",\"files\":[{\"path\":\"saves/Original/level.dat\"}]}");
    Path unrelated = game.resolve("power-beta-data/settings-backups/save-2"); Files.createDirectories(unrelated);
    Files.writeString(unrelated.resolve("complete.json"), "{\"config/power-beta.json\":\"file-0\"}");
    try (ZipFile zip = new ZipFile(run(WorldSnapshots.Action.BACKUP).path().toFile())) {
      assertNotNull(zip.getEntry("external/power-beta-data/settings-backups/save-1/file-0"));
      assertNotNull(zip.getEntry("external/power-beta-data/world-metadata-backups/upgrade-1/transaction.json"));
      assertNull(zip.getEntry("external/power-beta-data/settings-backups/save-2/complete.json"));
    }
  }
  @Test void preparedInventoryRecordsAreBackedUpWithoutReplayOrNewRecoveryOwnership() throws Exception {
    Path record = game.resolve("power-beta-data/inventory-recovery/prepared.journal");
    byte[] payload = ("{\"format\":1,\"phase\":\"prepared\",\"world\":\"" + id + "\",\"before\":[],\"after\":[]}").getBytes(StandardCharsets.UTF_8);
    journal(record, 0x4c504b48, payload); byte[] original = Files.readAllBytes(record);
    try (ZipFile zip = new ZipFile(run(WorldSnapshots.Action.BACKUP).path().toFile())) {
      assertArrayEquals(original, zip.getInputStream(zip.getEntry("external/power-beta-data/inventory-recovery/prepared.journal")).readAllBytes());
    }
    assertTrue(Files.isDirectory(run(WorldSnapshots.Action.DUPLICATE).path()));
    assertArrayEquals(original, Files.readAllBytes(record));
    try (var files = Files.list(record.getParent())) { assertEquals(1, files.count()); }
  }
}
