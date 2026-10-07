package local.luke.power.config;

import com.google.gson.*;
import java.nio.file.*;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
  @TempDir static Path game;
  private void checkFocusMigration() throws Exception {
    for (boolean pause : new boolean[]{false, true}) for (boolean active : new boolean[]{false, true}) {
      Path instance = game;
      java.util.Set<Path> previousBackups;
      try (var paths = Files.list(instance.resolve("power-beta-data/config-backups"))) {
        previousBackups = paths.collect(java.util.stream.Collectors.toSet());
      }
      ConfigMigration.prepare(instance);
      JsonObject before = PowerConfig.document();
      before.getAsJsonObject("settings").getAsJsonObject("power_controls:general").addProperty("pauseOnLostFocus", pause);
      before.getAsJsonObject("settings").getAsJsonObject("power_environment:config").addProperty("forceDisplayActive", active);
      before.getAsJsonObject("settings").addProperty("unrelatedSentinel", "preserved");
      PowerConfig.write(before);
      byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigMigration.prepare(instance);
      JsonObject expected = before.deepCopy();
      expected.getAsJsonObject("settings").getAsJsonObject("power_environment:config").remove("forceDisplayActive");
      expected.getAsJsonObject("settings").getAsJsonObject("power_controls:general").addProperty("pauseOnLostFocus", pause && !active);
      assertEquals(expected, PowerConfig.document(), "Only the two focus preferences may change");
      try (var paths = Files.list(instance.resolve("power-beta-data/config-backups"))) {
        var backups = paths.filter(p -> !previousBackups.contains(p)).toList();
        assertEquals(1, backups.size());
        try (var zip = new java.util.zip.ZipFile(backups.get(0).toFile())) {
          assertArrayEquals(original, zip.getInputStream(zip.getEntry("config/power-beta.json")).readAllBytes());
        }
      }
      byte[] migrated = Files.readAllBytes(PowerConfig.path());
      ConfigMigration.prepare(instance);
      assertArrayEquals(migrated, Files.readAllBytes(PowerConfig.path()), "Restart must not repeat migration");
    }
  }

  @Test void importsLegacyPreferencesOnceWithVerifiedBackup() throws Exception {
    Files.createDirectories(game.resolve("config/unitweaks"));
    Files.writeString(game.resolve("config/unitweaks/general.yml"),"rawInput: false\n");
    Files.writeString(game.resolve("options.txt"),"mouseSensitivity:0.4\nkey_key.unitweaks.hide_hud:59\nkey_Auto-walk (toggle):23\n");
    Files.writeString(game.resolve("config/lpketweaks.properties"),"autoWalk=true\nplacement.slabMode=MATCH_FIRST\nboatSteering=true\n");
    ConfigMigration.prepare(game);
    assertEquals(3,PowerConfig.document().get("schemaVersion").getAsInt());
    assertTrue(PowerConfig.section("building").get("autoWalk").getAsBoolean());
    assertTrue(PowerConfig.section("building").get("boatProtection").getAsBoolean());
    assertEquals("MATCH_FIRST",PowerConfig.section("building").getAsJsonObject("placement").get("slabMode").getAsString());
    assertEquals(59,PowerConfig.section("native").get("key_key.power_controls.hide_hud").getAsInt());
    assertFalse(PowerConfig.section("power_controls:general").get("rawInput").getAsBoolean());
    assertFalse(Files.exists(game.resolve("options.txt")));
    try(var files=Files.walk(game.resolve("config"))) { assertEquals(1,files.filter(Files::isRegularFile).count()); }
    try(var files=Files.list(game.resolve("power-beta-data/config-backups"))) { assertEquals(1,files.count()); }
    String bytes=Files.readString(PowerConfig.path());ConfigMigration.prepare(game);assertEquals(bytes,Files.readString(PowerConfig.path()));
    // Simulate interruption after the new document was committed but before old files were removed.
    Path legacy=game.resolve("config/interrupted.properties");byte[] original="kept=true".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(legacy,original);
    JsonObject pending=new JsonObject(),files=new JsonObject();
    files.addProperty("config/interrupted.properties",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(original)));
    pending.add("files",files);pending.add("settings",PowerConfig.document());
    Path marker=game.resolve("power-beta-data/pending-config-import.json");Files.writeString(marker,new Gson().toJson(pending));
    ConfigMigration.prepare(game);assertFalse(Files.exists(legacy));assertFalse(Files.exists(marker));
    assertEquals(bytes,Files.readString(PowerConfig.path()));
    checkFocusMigration();
    Files.writeString(PowerConfig.path(),"invalid");
    assertThrows(IllegalStateException.class,()->ConfigMigration.prepare(game));
    assertEquals("invalid",Files.readString(PowerConfig.path()));
  }
}
