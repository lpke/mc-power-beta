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
    assertEquals(3, root.get("schemaVersion").getAsInt());
    assertEquals(2, root.size());
    assertTrue(root.getAsJsonObject("settings").getAsJsonObject("creative").get("flight").getAsBoolean());
    assertFalse(root.getAsJsonObject("settings").getAsJsonObject("visual").get("inventoryInPortals").getAsBoolean());
    assertFalse(Catalog.JSON.fromJson("{}", local.luke.power.visual.VisualSettings.class).inventoryInPortals);
    assertFalse(root.getAsJsonObject("settings").getAsJsonObject("visual").get("shiftClickIntoCraftingGrid").getAsBoolean());
    assertFalse(Catalog.JSON.fromJson("{}", local.luke.power.visual.VisualSettings.class).shiftClickIntoCraftingGrid);
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
    for (String invalid : List.of("broken", "{\"schemaVersion\":2,\"settings\":{}}")) {
      Files.writeString(config, invalid);
      assertThrows(IllegalStateException.class, () -> ConfigDefaults.prepare(game));
      assertEquals(invalid, Files.readString(config));
    }
    Files.delete(config);
    ConfigDefaults.prepare(game);
    assertEquals(defaults, Files.readString(config));
  }
}
