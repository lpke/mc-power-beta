package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.config.backend.SavedWorldBackend;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class SavedWorldRulesTest {
  @TempDir Path game;
  private Path level, old;
  private Path previousConfig;
  @BeforeEach void setup() throws Exception {
    PowerConfig.flushPending();
    var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true);
    previousConfig = (Path)field.get(null); field.set(null, null);
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    Path world = game.resolve("saves/Example"); Files.createDirectories(world);
    level = world.resolve("level.dat"); old = world.resolve("level.dat_old");
    NbtCompound root = new NbtCompound(), data = new NbtCompound(), player = new NbtCompound();
    data.putString("LevelName", "Example"); data.putLong("Time", 987654321L);
    data.putInt("PowerBetaDifficulty", 3); data.putString("CustomWorldTag", "retain this");
    player.putString("PowerBetaGameMode", "SURVIVAL"); player.putString("NativeInventoryData", "untouched");
    data.put("Player", player); WorldRuleData.write(data, new RuleSet()); root.put("Data", data);
    write(level, root); Files.copy(level, old);
  }
  @AfterEach void restoreConfig() throws Exception {
    PowerConfig.flushPending();
    var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true); field.set(null, previousConfig);
  }
  private void write(Path path, NbtCompound root) throws Exception {
    try (var out = Files.newOutputStream(path)) { NbtIo.writeCompressed(root, out); }
  }
  private SavedWorldRules open() throws Exception { return new SavedWorldRules(game, "Example", () -> true); }
  private void edit(ConfigSession session, GameRule rule, boolean value) {
    session.settings().stream().filter(s -> s.id.equals("rules.world." + rule.id())).findFirst().orElseThrow().value = new JsonPrimitive(value);
  }

  @Test void savesOnlyRulesWithVerifiedBackupAndSupportsRepeatedEdits() throws Exception {
    byte[] original = Files.readAllBytes(level), previous = Files.readAllBytes(old);
    var session = SavedWorldBackend.open(open());
    edit(session, GameRule.ALLOW_CREATIVE_MODE, true); session.save(game);
    edit(session, GameRule.ALLOW_SLEEP, false); session.save(game);
    var data = WorldRuleFiles.read(level).getCompound("Data");
    RuleSet rules = WorldRuleData.read(data);
    assertTrue(rules.enabled(GameRule.ALLOW_CREATIVE_MODE)); assertFalse(rules.enabled(GameRule.ALLOW_SLEEP));
    assertEquals(987654321L, data.getLong("Time")); assertEquals(3, data.getInt("PowerBetaDifficulty"));
    assertEquals("retain this", data.getString("CustomWorldTag"));
    assertEquals("SURVIVAL", data.getCompound("Player").getString("PowerBetaGameMode"));
    assertEquals("untouched", data.getCompound("Player").getString("NativeInventoryData"));
    assertArrayEquals(previous, Files.readAllBytes(old));
    try (var files = Files.walk(game.resolve("power-beta-data/settings-backups"))) {
      var manifests = files.filter(p -> p.getFileName().toString().equals("complete.json")).toList();
      assertEquals(2, manifests.size());
      assertTrue(manifests.stream().anyMatch(p -> {
        try {
          JsonObject manifest = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
          return Arrays.equals(original, Files.readAllBytes(p.getParent().resolve(manifest.get("saves/Example/level.dat").getAsString())));
        } catch (Exception e) { throw new RuntimeException(e); }
      }));
    }
  }
  @Test void viewingAndDiscardingNeverWritesMetadata() throws Exception {
    byte[] original = Files.readAllBytes(level);
    var session = SavedWorldBackend.open(open()); edit(session, GameRule.ALLOW_SLEEP, false); session.discard();
    assertArrayEquals(original, Files.readAllBytes(level));
    assertFalse(Files.exists(game.resolve("power-beta-data/settings-backups")));
  }
  @Test void sprintRuleDefaultsOffForOlderWorldsDespiteGlobalEnablementAndSurvivesSavedEdits() throws Exception {
    var preferences = new local.luke.power.sprint.SprintSettings();
    preferences.enabled = true;
    preferences.speed = 145;
    preferences.stamina = true;
    PowerConfig.save("sprint", preferences);
    byte[] global = Files.readAllBytes(PowerConfig.path());
    var root = WorldRuleFiles.read(level);
    var data = root.getCompound("Data");
    data.putString("PowerBetaCommandWorldId", "stable-sprint-world");
    for (int version = 1; version <= RuleSet.VERSION; version++) {
      var oldRules = new NbtCompound();
      oldRules.putInt("version", version);
      oldRules.putBoolean("allow_creative_mode", true);
      data.put("GameRules", oldRules);
      assertFalse(WorldRuleData.read(data).enabled(GameRule.ALLOW_SPRINTING));
    }
    write(level, root);
    byte[] previous = Files.readAllBytes(old);
    var session = SavedWorldBackend.open(open());
    edit(session, GameRule.ALLOW_SPRINTING, true);
    session.save(game);
    data = WorldRuleFiles.read(level).getCompound("Data");
    assertTrue(WorldRuleData.read(data).enabled(GameRule.ALLOW_SPRINTING));
    assertTrue(WorldRuleData.read(data).enabled(GameRule.ALLOW_CREATIVE_MODE));
    assertEquals("stable-sprint-world", data.getString("PowerBetaCommandWorldId"));
    assertEquals(3, data.getInt("PowerBetaDifficulty"));
    assertEquals("SURVIVAL", data.getCompound("Player").getString("PowerBetaGameMode"));
    assertArrayEquals(previous, Files.readAllBytes(old));
    assertArrayEquals(global, Files.readAllBytes(PowerConfig.path()));
    var restarted = SavedWorldBackend.open(open());
    edit(restarted, GameRule.ALLOW_SPRINTING, false);
    restarted.save(game);
    assertFalse(open().rules().enabled(GameRule.ALLOW_SPRINTING));
    assertArrayEquals(global, Files.readAllBytes(PowerConfig.path()));
  }
  @Test void renamePreservesRulesPlayerDataAndPreviousMetadataAndRejectsConcurrentChanges() throws Exception {
    byte[] previous = Files.readAllBytes(old);
    var world = open(); var rules = world.rules();
    world.rename("  A new name  ");
    assertEquals("A new name", world.name());
    var data = WorldRuleFiles.read(level).getCompound("Data");
    assertEquals("A new name", data.getString("LevelName"));
    assertEquals(rules, WorldRuleData.read(data));
    assertEquals("untouched", data.getCompound("Player").getString("NativeInventoryData"));
    assertEquals("SURVIVAL", data.getCompound("Player").getString("PowerBetaGameMode"));
    assertEquals(987654321L, data.getLong("Time"));
    assertArrayEquals(previous, Files.readAllBytes(old));
    byte[] named = Files.readAllBytes(level);
    assertThrows(IOException.class, () -> world.rename("   ")); assertArrayEquals(named, Files.readAllBytes(level));
    var external = WorldRuleFiles.read(level); external.getCompound("Data").putString("LevelName", "External edit"); write(level, external);
    byte[] changed = Files.readAllBytes(level);
    assertThrows(IOException.class, () -> world.rename("Replace")); assertArrayEquals(changed, Files.readAllBytes(level));
  }
  @Test void changedMetadataAndLoadedWorldsBlockWrites() throws Exception {
    var session = SavedWorldBackend.open(open()); edit(session, GameRule.ALLOW_SLEEP, false);
    var root = WorldRuleFiles.read(level); root.getCompound("Data").putLong("Time", 333); write(level, root);
    byte[] external = Files.readAllBytes(level);
    assertThrows(IOException.class, () -> session.save(game)); assertArrayEquals(external, Files.readAllBytes(level));
    assertThrows(IOException.class, () -> new SavedWorldRules(game, "Example", () -> false));
  }
  @Test void futureRulesAndPathsOutsideTheWorldAreRejectedWithoutWrites() throws Exception {
    var root = WorldRuleFiles.read(level); root.getCompound("Data").getCompound("GameRules").putInt("version", 999); write(level, root);
    byte[] future = Files.readAllBytes(level);
    assertThrows(IllegalArgumentException.class, this::open); assertArrayEquals(future, Files.readAllBytes(level));
    assertThrows(IOException.class, () -> new SavedWorldRules(game, "../Example", () -> true));
    Files.delete(level); Files.createSymbolicLink(level, old);
    assertThrows(IOException.class, this::open);
  }
  @Test void missingPrimaryUsesThePreviousMetadataWithoutReplacingThePreviousFile() throws Exception {
    byte[] previous = Files.readAllBytes(old); Files.delete(level);
    var session = SavedWorldBackend.open(open()); edit(session, GameRule.ALLOW_SLEEP, false); session.save(game);
    assertFalse(WorldRuleData.read(WorldRuleFiles.read(level).getCompound("Data")).enabled(GameRule.ALLOW_SLEEP));
    assertArrayEquals(previous, Files.readAllBytes(old));
  }
  @Test void laterBackendFailureRestoresThePreviousRulesThroughADurableSave() throws Exception {
    byte[] original = Files.readAllBytes(level);
    var session = SavedWorldBackend.open(open());
    session.add(new Backend() {
      public String id() { return "fail"; }
      public List<Path> files() { return List.of(); }
      public void validate(Map<String, JsonElement> values) {}
      public void apply(Map<String, JsonElement> values) throws Exception { throw new IOException("fixture failure"); }
      public void restore(Map<String, JsonElement> values) {}
    }, List.of(new Setting("failure", "fail", "Game rules", "", "Failure", "", Setting.Kind.BOOLEAN,
        new JsonPrimitive(false), new JsonPrimitive(false), 0, 1, 1, List.of(), false)));
    edit(session, GameRule.ALLOW_SLEEP, false); session.settings().get(session.settings().size()-1).value = new JsonPrimitive(true);
    assertThrows(IOException.class, () -> session.save(game));
    assertArrayEquals(original, Files.readAllBytes(level));
  }
}
