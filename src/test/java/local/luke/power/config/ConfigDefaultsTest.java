package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import java.util.List;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigDefaultsTest {
  @TempDir Path game;

  @Test void defaultsAndRecoveryUseOnlyTheCurrentDocument() throws Exception {
    Path obsolete = game.resolve("config/lpkecreative.properties");
    Files.createDirectories(obsolete.getParent());
    Files.writeString(obsolete, "flight=false\n");
    ConfigDefaults.prepare(game);
    Path config = PowerConfig.path();
    String defaults = Files.readString(config);
    JsonObject root = PowerConfig.document();
    assertEquals(local.luke.power.storage.ConfigVersions.CURRENT, root.get("schemaVersion").getAsInt());
    assertEquals(2, root.size());
    var settings = root.getAsJsonObject("settings");
    assertFalse(settings.getAsJsonObject("power_controls:userinterface").get("disableDebugEntityIdTags").getAsBoolean());
    for (String key : List.of("disableEntityCulling", "disableBlockEntityCulling")) {
      boolean disabled = settings.getAsJsonObject("entityculling:config").get(key).getAsBoolean();
      assertFalse(disabled, "fresh installs must enable culling");
      assertEquals(!disabled, Catalog.metadata("entityculling:config." + key).get("default").getAsBoolean());
    }
    assertTrue(settings.getAsJsonObject("gameplay").get("limitTnt").getAsBoolean());
    assertTrue(Catalog.metadata("gameplay.limitTnt").get("default").getAsBoolean());
    assertTrue(settings.getAsJsonObject("interface").get("useFeetYCoordinates").getAsBoolean());
    assertTrue(settings.getAsJsonObject("interface").get("blinkingUiCursor").getAsBoolean());
    assertFalse(settings.getAsJsonObject("interface").get("hidePowerBetaVersion").getAsBoolean());
    assertEquals(local.luke.power.ui.DebugOrder.defaults(), local.luke.power.ui.DebugOrder.read(settings.getAsJsonObject("interface").get("debugOrder")));
    assertEquals(local.luke.power.ui.DebugColumns.defaultRight(), local.luke.power.ui.DebugOrder.read(settings.getAsJsonObject("interface").get("debugRightOrder")));
    assertFalse(settings.getAsJsonObject("power_controls:userinterface").getAsJsonObject("panoramaConfig").get("onlyTitleScreen").getAsBoolean());
    assertFalse(settings.getAsJsonObject("power_controls:userinterface").has("achievementBackToMenu"));
    assertTrue(settings.getAsJsonObject("power_environment:config").getAsJsonObject("WEATHER_CONFIG").get("sleepOnlyResetsWeatherWhenRaining").getAsBoolean());
    try (var metadata = getClass().getResourceAsStream("/fabric.mod.json")) {
      String version = JsonParser.parseReader(new java.io.InputStreamReader(metadata, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get("version").getAsString();
      assertEquals("Power Beta " + version, settings.getAsJsonObject("power_controls:userinterface").getAsJsonObject("versionTextConfig").get("customVersionText").getAsString());
      assertEquals("Power Beta " + version, Catalog.metadata("power_controls:userinterface.versionTextConfig.customVersionText").get("default").getAsString());
    }
    assertFalse(settings.getAsJsonObject("gameplay").get("torchShovel").getAsBoolean());
    assertFalse(settings.getAsJsonObject("gameplay").has("fireSpread"));
    assertTrue(local.luke.power.rules.RulePresets.survival().enabled(local.luke.power.rules.GameRule.FIRE_SPREAD));
    assertTrue(settings.getAsJsonObject("activeTweaks").get("clickMining").getAsBoolean());
    assertFalse(settings.getAsJsonObject("veinMining").get("enabled").getAsBoolean());
    assertEquals(25, settings.getAsJsonObject("veinMining").get("timePercent").getAsInt());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("building").get("inventoryMouseLook").getAsBoolean());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("death").get("enabled").getAsBoolean());
    assertFalse(root.getAsJsonObject("settings").getAsJsonObject("death").get("announceItems").getAsBoolean());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("power_mechanics:config")
        .getAsJsonObject("MOB_CONFIG").get("waterNegatesFallDamage").getAsBoolean());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("power_compat:config")
        .getAsJsonObject("STACK_SIZE_CONFIG").get("stackableEmptyBuckets").getAsBoolean());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("creative").get("flight").getAsBoolean());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("visual").get("inventoryInPortals").getAsBoolean());
    assertTrue(Catalog.JSON.fromJson("{}", local.luke.power.visual.VisualSettings.class).inventoryInPortals);
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("visual").get("shiftClickIntoCraftingGrid").getAsBoolean());
    assertTrue(Catalog.JSON.fromJson("{}", local.luke.power.visual.VisualSettings.class).shiftClickIntoCraftingGrid);
    assertFalse(root.getAsJsonObject("settings").getAsJsonObject("visual").get("lowFire").getAsBoolean());
    for (String key : List.of("damageCameraShake", "fireDamageCameraShake"))
      assertTrue(root.getAsJsonObject("settings").getAsJsonObject("visual").get(key).getAsBoolean());
    var oldVisual = Catalog.JSON.fromJson("{}", local.luke.power.visual.VisualSettings.class);
    assertFalse(oldVisual.lowFire);
    assertTrue(oldVisual.damageCameraShake);
    assertTrue(oldVisual.fireDamageCameraShake);
    assertEquals("flight=false\n", Files.readString(obsolete));

    root.getAsJsonObject("settings").getAsJsonObject("creative").addProperty("flight", false);
    PowerConfig.write(root);
    String preferences = Files.readString(config);
    ConfigDefaults.prepare(game);
    assertEquals(preferences, Files.readString(config));
    JsonObject toggle = new JsonObject(); toggle.addProperty("enabled", true);
    PowerConfig.saveDeferred("queued-toggle", toggle);
    try (var transaction = FileTransaction.begin(game, List.of(config))) {
      assertTrue(PowerConfig.section("queued-toggle").get("enabled").getAsBoolean());
      toggle.addProperty("enabled", false); PowerConfig.put("queued-toggle", toggle);
      // Closing an uncommitted menu save restores the already-flushed gameplay setting.
    }
    assertTrue(PowerConfig.section("queued-toggle").get("enabled").getAsBoolean());
    root = PowerConfig.document(); root.getAsJsonObject("settings").remove("queued-toggle"); PowerConfig.write(root);
    assertEquals(preferences, Files.readString(config));
    FileTransaction.begin(game, List.of(config)); // Simulate process exit before commit/close.
    Files.writeString(config, "interrupted");
    FileTransaction.recover(game);
    assertEquals(preferences, Files.readString(config));
    Files.delete(config);
    ConfigDefaults.prepare(game);
    assertEquals(defaults, Files.readString(config));

    var veins = PowerConfig.read("veinMining", local.luke.power.mining.VeinSettings.class);
    veins.enabled = true; veins.timePercent = 37; veins.tools.put("12", List.of(277));
    PowerConfig.save("veinMining", veins);
    byte[] saved = Files.readAllBytes(config);
    ConfigDefaults.prepare(game);
    var reloaded = PowerConfig.read("veinMining", local.luke.power.mining.VeinSettings.class);
    reloaded.validate(); assertTrue(reloaded.enabled && reloaded.accepts(12, 277));
    assertEquals(37, reloaded.timePercent);
    try (var transaction = FileTransaction.begin(game, List.of(config))) {
      veins.timePercent = 99; PowerConfig.save("veinMining", veins);
    }
    assertArrayEquals(saved, Files.readAllBytes(config));
    FileTransaction.begin(game, List.of(config));
    veins.tools.clear(); PowerConfig.save("veinMining", veins);
    FileTransaction.recover(game); ConfigDefaults.prepare(game);
    assertArrayEquals(saved, Files.readAllBytes(config));
    assertTrue(PowerConfig.read("veinMining", local.luke.power.mining.VeinSettings.class).accepts(12, 277));
    // Bootstrap must let the read-only error UI start without replacing unsupported data.
    for (String invalid : List.of("broken", "{\"schemaVersion\":2,\"settings\":{}}", "{\"schemaVersion\":99,\"settings\":{}}")) {
      Files.writeString(config, invalid);
      try {
        ConfigDefaults.prepare(game);
        assertFalse(PowerConfig.problem().isEmpty());
        assertEquals(local.luke.power.storage.ConfigVersions.CURRENT, PowerConfig.document().get("schemaVersion").getAsInt());
        assertThrows(java.io.IOException.class, () -> PowerConfig.write(rootCopy()));
        assertEquals(invalid, Files.readString(config));
      } finally {
        // Simulate a new process without exposing a production unlock operation.
        var recovery = PowerConfig.class.getDeclaredField("recoveryDefaults"); recovery.setAccessible(true); recovery.set(null, null);
        var problem = PowerConfig.class.getDeclaredField("problem"); problem.setAccessible(true); problem.set(null, "");
      }
    }
  }
  private JsonObject rootCopy() { return PowerConfig.document(); }
}
