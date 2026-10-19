package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.SettingsDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultBasesTest {
  @TempDir Path game;
  private Object priorFile, priorBase, priorSetup;
  @org.junit.jupiter.api.BeforeEach void isolateProcessState() throws Exception {
    PowerConfig.flushPending();
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); priorFile = file.get(null); file.set(null, null);
    var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); priorBase = base.get(null);
    var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); priorSetup = setup.get(null);
  }
  @org.junit.jupiter.api.AfterEach void restoreProcessState() throws Exception {
    PowerConfig.flushPending();
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); file.set(null, priorFile);
    var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); base.set(null, priorBase);
    var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); setup.set(null, priorSetup);
  }
  @Test void changingBaseOnlyWritesMetadataAndRefreshesResetTargets() throws Exception {
    ConfigDefaults.prepare(game);
    JsonObject before = PowerConfig.document().getAsJsonObject("settings").deepCopy();
    Setting setting = new Setting("power_gold:config.enableGoldSwordLooting", "test", "", "", "Looting", "", Setting.Kind.BOOLEAN,
        new JsonPrimitive(true), new JsonPrimitive(true), 0, 1, 1, List.of(), false);
    try {
      DefaultBases.use(DefaultBases.Base.BETA, game);
      setting.refreshDefault();
      assertTrue(setting.value.getAsBoolean()); assertFalse(setting.changed()); assertFalse(setting.defaultValue.getAsBoolean());
      JsonObject after = PowerConfig.document().getAsJsonObject("settings").deepCopy();
      before.remove("defaults"); after.remove("defaults"); assertEquals(before, after);
      SettingsDraft nested = new SettingsDraft(List.of(setting), s -> true);
      DefaultBases.use(DefaultBases.Base.POWER_BETA, game);
      nested.settings().forEach(Setting::refreshDefault);
      assertTrue(nested.get(setting.id).defaultValue.getAsBoolean());
      assertTrue(nested.get(setting.id).value.getAsBoolean());
      DefaultBases.reload(); assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.current());
      JsonObject applied = PowerConfig.document().getAsJsonObject("settings").deepCopy();
      applied.remove("defaults"); assertEquals(before, applied);
    } finally { DefaultBases.use(DefaultBases.Base.POWER_BETA, game); }
  }
  @Test void missingBoatDirectionIsInitializedOnceAndResetBaseNeverChangesItsSavedValue() throws Exception {
    ConfigDefaults.prepare(game);
    for (var base : DefaultBases.Base.values()) {
      JsonObject old = PowerConfig.document();
      old.getAsJsonObject("settings").getAsJsonObject("defaults").addProperty("base",base.id);
      old.getAsJsonObject("settings").getAsJsonObject("gameplay").remove("boatPlacementFacing");
      old.addProperty("preservedEvidence", "unchanged");
      PowerConfig.write(old);byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      boolean enabled = base != DefaultBases.Base.BETA;
      JsonObject expected = old.deepCopy();
      expected.getAsJsonObject("settings").getAsJsonObject("gameplay").addProperty("boatPlacementFacing", enabled);
      assertEquals(expected,PowerConfig.document());
      assertEquals(enabled,DefaultBases.value(base,"gameplay.boatPlacementFacing",new JsonPrimitive(true)).getAsBoolean());
      try (var dirs=Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(dirs.anyMatch(dir->{try{return java.util.Arrays.equals(original,Files.readAllBytes(dir.resolve("file-0"))) && Files.exists(dir.resolve("complete.json"));}catch(Exception e){return false;}}));
      }
      DefaultBases.use(enabled ? DefaultBases.Base.BETA : DefaultBases.Base.POWER_BETA,game);
      byte[] saved=Files.readAllBytes(PowerConfig.path());ConfigDefaults.prepare(game);
      assertArrayEquals(saved,Files.readAllBytes(PowerConfig.path()));
      assertEquals(enabled,PowerConfig.section("gameplay").get("boatPlacementFacing").getAsBoolean());
    }
  }
  @Test void minimapUsesItsInitialBaseAndSurvivesBaseChangesRollbackAndRestart() throws Exception {
    ConfigDefaults.prepare(game);
    for (var base : DefaultBases.Base.values()) {
      JsonObject old = PowerConfig.document();
      var settings = old.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base", base.id);
      settings.getAsJsonObject("maps").remove("minimap");
      settings.getAsJsonObject("maps").addProperty("retainedUnknown", "unchanged");
      PowerConfig.write(old);
      byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      boolean enabled = base == DefaultBases.Base.POWER_BETA;
      JsonObject expected = old.deepCopy();
      expected.getAsJsonObject("settings").getAsJsonObject("maps").addProperty("minimap", enabled);
      assertEquals(expected, PowerConfig.document());
      try (var dirs = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(dirs.anyMatch(dir -> {
          try { return Arrays.equals(original, Files.readAllBytes(dir.resolve("file-0"))) && Files.exists(dir.resolve("complete.json")); }
          catch (Exception e) { return false; }
        }));
      }
      DefaultBases.use(enabled ? DefaultBases.Base.BETA : DefaultBases.Base.POWER_BETA, game);
      byte[] saved = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      assertArrayEquals(saved, Files.readAllBytes(PowerConfig.path()));
      try (var transaction = FileTransaction.begin(game, List.of(PowerConfig.path()))) {
        JsonObject maps = PowerConfig.section("maps");
        maps.addProperty("minimap", !enabled);
        PowerConfig.put("maps", maps);
      }
      ConfigDefaults.prepare(game);
      assertArrayEquals(saved, Files.readAllBytes(PowerConfig.path()));
      assertEquals(enabled, PowerConfig.section("maps").get("minimap").getAsBoolean());
    }
  }
  @Test void waterMovementUsesItsInitialBaseAndSurvivesBaseChangesRollbackAndRestart() throws Exception {
    ConfigDefaults.prepare(game);
    for(var base:DefaultBases.Base.values()) {
      var old=PowerConfig.document();var settings=old.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base",base.id);
      settings.getAsJsonObject("gameplay").remove("modernWaterMovement");
      PowerConfig.write(old);byte[] bytes=Files.readAllBytes(PowerConfig.path());ConfigDefaults.prepare(game);
      boolean enabled=base==DefaultBases.Base.POWER_BETA;
      var expected=old.deepCopy();expected.getAsJsonObject("settings").getAsJsonObject("gameplay").addProperty("modernWaterMovement",enabled);
      assertEquals(expected,PowerConfig.document());
      assertEquals(enabled,PowerConfig.read("gameplay",local.luke.power.gameplay.GameplaySettings.class).modernWaterMovement);
      try(var dirs=Files.list(game.resolve("power-beta-data/settings-backups"))){
        assertTrue(dirs.anyMatch(dir->{try{return Arrays.equals(bytes,Files.readAllBytes(dir.resolve("file-0")))&&Files.exists(dir.resolve("complete.json"));}catch(Exception e){return false;}}));
      }
      DefaultBases.use(enabled?DefaultBases.Base.BETA:DefaultBases.Base.POWER_BETA,game);
      byte[] applied=Files.readAllBytes(PowerConfig.path());ConfigDefaults.prepare(game);assertArrayEquals(applied,Files.readAllBytes(PowerConfig.path()));
      assertEquals(enabled,PowerConfig.section("gameplay").get("modernWaterMovement").getAsBoolean());
      try(var transaction=FileTransaction.begin(game,List.of(PowerConfig.path()))) {
        var draft=PowerConfig.read("gameplay",local.luke.power.gameplay.GameplaySettings.class);draft.modernWaterMovement=!enabled;PowerConfig.save("gameplay",draft);
      }
      assertArrayEquals(applied,Files.readAllBytes(PowerConfig.path()));ConfigDefaults.prepare(game);
      assertEquals(enabled,PowerConfig.read("gameplay",local.luke.power.gameplay.GameplaySettings.class).modernWaterMovement);
    }
  }
  @Test void consolidatedPowerBetaRetainsBothIdsAndUsesStandardDisplayDefaults() throws Exception {
    ConfigDefaults.prepare(game);
    assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.Base.read("power_beta"));
    assertEquals("Power Beta", DefaultBases.Base.POWER_BETA.label);
    assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.Base.values()[0]);
    assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.Base.read("power_beta_full"));
    assertEquals(3, DefaultBases.Base.values().length);
    for (var id : List.of("veinMining.enabled", "gameplay.torchShovel", "gameplay.axesSlashLeaves", "tweaks.boatSpeed")) {
      assertTrue(DefaultBases.value(DefaultBases.Base.POWER_BETA, id, new JsonPrimitive(false)).getAsBoolean(), id);
    }
    var nativeDefaults = PowerConfig.section("native");
    assertEquals(4, nativeDefaults.get("guiScale").getAsInt());
    assertEquals(.5, nativeDefaults.get("gui_scale").getAsDouble());
    var panorama = PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig");
    var blurDefault = Catalog.metadata("power_controls:userinterface.panoramaConfig.blurBackground").get("default");
    assertTrue(panorama.get("blurBackground").getAsBoolean());
    assertTrue(panorama.get("enablePanorma").getAsBoolean());
    for (var base : DefaultBases.Base.values()) {
      for (String id : List.of("creative.sprintToggle", "creative.spectatorSprintToggle", "power_camera:config.sprintToggle"))
        assertTrue(DefaultBases.value(base,id,new JsonPrimitive(false)).getAsBoolean(),base + " " + id);
      assertEquals(4, DefaultBases.value(base, "native.guiScale", new JsonPrimitive(4)).getAsInt());
      assertTrue(DefaultBases.value(base, "power_controls:userinterface.panoramaConfig.blurBackground", blurDefault).getAsBoolean());
      assertEquals(base == DefaultBases.Base.POWER_BETA ? "b1_8_1" : "b1_7_3",
          DefaultBases.value(base, "power_controls:userinterface.panoramaConfig.panoramaFolder", new JsonPrimitive("b1_7_3")).getAsString());
    }
    assertTrue(PowerConfig.section("creative").get("sprintToggle").getAsBoolean());
    assertTrue(PowerConfig.section("creative").get("spectatorSprintToggle").getAsBoolean());
  }
  @Test void powerBetaKeepsOresAndStandardControlsWhileUsingClearerWater() throws Exception {
    ConfigDefaults.prepare(game);
    JsonObject nativeValues = PowerConfig.section("native");
    assertEquals(.5, nativeValues.get("mouseSensitivity").getAsDouble());
    assertEquals(0, nativeValues.get("fov").getAsDouble());
    assertEquals(0, nativeValues.get("brightness").getAsDouble());
    assertEquals(0, nativeValues.get("cloud_height").getAsDouble());
    assertEquals(1, nativeValues.get("music").getAsDouble());
    assertEquals(1, nativeValues.get("sound").getAsDouble());
    var veins = PowerConfig.section("veinMining");
    assertEquals(java.util.Set.of("16", "15", "14", "21", "56", "73"), veins.getAsJsonObject("tools").keySet());
    var creative = PowerConfig.section("creative");
    for (String id : List.of("acceleration", "glide", "spectatorAcceleration", "spectatorGlide"))
      assertEquals(5, creative.get(id).getAsDouble(), id);
    for (String id : List.of("flightSpeed", "spectatorSpeed")) assertEquals(100, creative.get(id).getAsInt());
    for (String id : List.of("sprintMultiplier", "spectatorSprintMultiplier")) assertEquals(200, creative.get(id).getAsInt());
    var water = PowerConfig.section("visual");
    assertEquals(30, water.get("underwaterFogDensity").getAsInt());
    assertEquals(75, water.get("underwaterDarkness").getAsInt());
    assertEquals(95, water.get("underwaterDarknessOutside").getAsInt());
    assertTrue(water.get("removeUnderwaterOverlay").getAsBoolean());
    assertEquals(1, PowerConfig.section("title").get("style").getAsInt());
    var alias = new DefaultBases.State(); alias.base = "power_beta_full"; alias.setupComplete = true;
    PowerConfig.save("defaults", alias); byte[] before = java.nio.file.Files.readAllBytes(PowerConfig.path());
    DefaultBases.reload(); assertEquals(DefaultBases.Base.POWER_BETA, DefaultBases.current());
    assertArrayEquals(before, java.nio.file.Files.readAllBytes(PowerConfig.path()));
  }
  @Test void existingInstallsSkipSetupAndFreshInstallsAskOnce() throws Exception {
    ConfigDefaults.prepare(game); assertTrue(DefaultBases.needsSetup());
    var state = new DefaultBases.State(); state.setupComplete = true; PowerConfig.save("defaults", state);
    ConfigDefaults.prepare(game); assertFalse(DefaultBases.needsSetup());
    JsonObject doc = PowerConfig.document(); doc.getAsJsonObject("settings").remove("defaults"); PowerConfig.write(doc);
    ConfigDefaults.prepare(game); assertFalse(DefaultBases.needsSetup());
    assertTrue(PowerConfig.section("defaults").get("setupComplete").getAsBoolean());
  }
  @Test void refinedAndBetaKeepGameplayVanillaWhileFixesAndInventoryDiffer() {
    var yes = new JsonPrimitive(true);
    for (var base : List.of(DefaultBases.Base.BETA, DefaultBases.Base.REFINED)) {
      assertFalse(DefaultBases.value(base, "power_gold:config.enableGoldSwordLooting", yes).getAsBoolean());
      assertTrue(DefaultBases.value(base, "rules.survival.player_crop_trampling", new JsonPrimitive(false)).getAsBoolean());
      assertEquals(30, DefaultBases.value(base, "rules.cheats.explosion_drop_chance", new JsonPrimitive(100)).getAsInt());
    }
    assertFalse(DefaultBases.value(DefaultBases.Base.BETA, "power_controls:bugfixes.miningDelayFix", yes).getAsBoolean());
    assertTrue(DefaultBases.value(DefaultBases.Base.REFINED, "power_controls:bugfixes.miningDelayFix", new JsonPrimitive(false)).getAsBoolean());
    assertFalse(DefaultBases.value(DefaultBases.Base.BETA, "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableLeftClickDrag", yes).getAsBoolean());
    assertTrue(DefaultBases.value(DefaultBases.Base.REFINED, "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableLeftClickDrag", yes).getAsBoolean());
  }
}
