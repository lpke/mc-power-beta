package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class TitleSplashDefaultsTest {
  @TempDir Path game;
  private Object file, base, setup;
  @BeforeEach void isolate() throws Exception {
    PowerConfig.flushPending(); file = field(PowerConfig.class, "file").get(null); field(PowerConfig.class, "file").set(null, null);
    base = field(DefaultBases.class, "current").get(null); setup = field(DefaultBases.class, "setupComplete").get(null);
  }
  @AfterEach void restore() throws Exception {
    PowerConfig.flushPending(); field(PowerConfig.class, "file").set(null, file);
    field(DefaultBases.class, "current").set(null, base); field(DefaultBases.class, "setupComplete").set(null, setup);
  }
  @Test void splashStartsOnInEveryBaseAndExistingInstallsGetOneBackedUpAddition() throws Exception {
    ConfigDefaults.prepare(game);
    assertTrue(PowerConfig.section("title").get("showSplashText").getAsBoolean());
    for (var selected : DefaultBases.Base.values()) {
      assertTrue(DefaultBases.value(selected, "logo.showSplashText", new JsonPrimitive(false)).getAsBoolean());
      JsonObject original = PowerConfig.document();
      original.getAsJsonObject("settings").getAsJsonObject("defaults").addProperty("base", selected.id);
      JsonObject title = original.getAsJsonObject("settings").getAsJsonObject("title");
      title.remove("showSplashText"); title.addProperty("unknownFutureTitleData", "retained");
      PowerConfig.write(original); byte[] before = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      JsonObject expected = original.deepCopy(); expected.getAsJsonObject("settings").getAsJsonObject("title").addProperty("showSplashText", true);
      assertEquals(expected, PowerConfig.document());
      try (var backups = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(backups.anyMatch(dir -> {
          try { return Files.exists(dir.resolve("complete.json")) && Arrays.equals(before, Files.readAllBytes(dir.resolve("file-0"))); }
          catch (Exception failure) { return false; }
        }), "verified original backup for " + selected);
      }
      byte[] initialized = Files.readAllBytes(PowerConfig.path()); ConfigDefaults.prepare(game);
      assertArrayEquals(initialized, Files.readAllBytes(PowerConfig.path()));
      JsonObject disabled = PowerConfig.section("title"); disabled.addProperty("showSplashText", false); PowerConfig.put("title", disabled);
      ConfigDefaults.prepare(game); assertEquals(disabled, PowerConfig.section("title"));
    }
  }
  private static java.lang.reflect.Field field(Class<?> type, String name) throws Exception { var f = type.getDeclaredField(name); f.setAccessible(true); return f; }
}
