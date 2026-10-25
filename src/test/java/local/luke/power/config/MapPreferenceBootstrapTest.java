package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class MapPreferenceBootstrapTest {
  @TempDir Path game;
  private Object priorFile, priorBase, priorSetup;
  @BeforeEach void isolate() throws Exception {
    PowerConfig.flushPending();
    var file=PowerConfig.class.getDeclaredField("file"); file.setAccessible(true);priorFile=file.get(null);file.set(null,null);
    var base=DefaultBases.class.getDeclaredField("current");base.setAccessible(true);priorBase=base.get(null);
    var setup=DefaultBases.class.getDeclaredField("setupComplete");setup.setAccessible(true);priorSetup=setup.get(null);
  }
  @AfterEach void restore() throws Exception {
    PowerConfig.flushPending();
    var file=PowerConfig.class.getDeclaredField("file");file.setAccessible(true);file.set(null,priorFile);
    var base=DefaultBases.class.getDeclaredField("current");base.setAccessible(true);base.set(null,priorBase);
    var setup=DefaultBases.class.getDeclaredField("setupComplete");setup.setAccessible(true);setup.set(null,priorSetup);
  }
  @Test void newMapPreferencesUseTheSelectedBaseOnceAndPreserveSavedChoices() throws Exception {
    for (var base : DefaultBases.Base.values()) {
      Path instance = game;
      ConfigDefaults.prepare(instance);
      JsonObject root = PowerConfig.document();
      var settings = root.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base", base.id);
      var maps = settings.getAsJsonObject("maps");
      maps.remove("photoWaypoints"); maps.remove("renderedArea");
      maps.addProperty("zoom", 2.5);
      PowerConfig.write(root);
      byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(instance);
      boolean expected = base == DefaultBases.Base.POWER_BETA;
      assertEquals(expected, PowerConfig.section("maps").get("photoWaypoints").getAsBoolean());
      assertFalse(PowerConfig.section("maps").get("renderedArea").getAsBoolean());
      assertEquals(2.5, PowerConfig.section("maps").get("zoom").getAsDouble());
      try (var backups = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(backups.anyMatch(directory -> {
          try { return Files.exists(directory.resolve("complete.json"))
              && java.util.Arrays.equals(original, Files.readAllBytes(directory.resolve("file-0"))); }
          catch (Exception failure) { return false; }
        }));
      }
      JsonObject saved = PowerConfig.section("maps");
      saved.addProperty("photoWaypoints", !expected); saved.addProperty("renderedArea", true);
      PowerConfig.put("maps", saved);
      byte[] before = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(instance);
      assertArrayEquals(before, Files.readAllBytes(PowerConfig.path()));
    }
  }

}
