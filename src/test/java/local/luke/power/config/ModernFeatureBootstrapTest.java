package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ModernFeatureBootstrapTest {
  @TempDir Path game;
  private Object priorFile, priorBase, priorSetup;
  @BeforeEach void isolate() throws Exception {
    PowerConfig.flushPending(); priorFile = field(PowerConfig.class, "file").get(null); field(PowerConfig.class, "file").set(null, null);
    priorBase = field(DefaultBases.class, "current").get(null); priorSetup = field(DefaultBases.class, "setupComplete").get(null);
  }
  @AfterEach void restore() throws Exception {
    PowerConfig.flushPending(); field(PowerConfig.class, "file").set(null, priorFile);
    field(DefaultBases.class, "current").set(null, priorBase); field(DefaultBases.class, "setupComplete").set(null, priorSetup);
  }
  @Test void missingSwitchesInitializeOnceWithVerifiedBackupAndPreserveOpaqueUserData() throws Exception {
    ConfigDefaults.prepare(game);
    for (var base : DefaultBases.Base.values()) {
      JsonObject old = PowerConfig.document(), settings = old.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base", base.id);
      for (String id : ModernFeatureIntegrationTest.FLAGS) { int split = id.indexOf('.'); settings.getAsJsonObject(id.substring(0, split)).remove(id.substring(split + 1)); }
      JsonObject presets = new JsonObject(); presets.addProperty("format", 999); presets.addProperty("untouched", "opaque user data"); settings.add("savedCreativeHotbars", presets);
      settings.getAsJsonObject("native").addProperty("fov", "0.431"); settings.getAsJsonObject("native").addProperty("guiScale", "3"); settings.getAsJsonObject("native").addProperty("fpsLimit", "2");
      settings.getAsJsonObject("native").addProperty("gui_scale", "0.375"); settings.getAsJsonObject("native").addProperty("framerate_limit", 145);
      PowerConfig.write(old); byte[] original = Files.readAllBytes(PowerConfig.path()); ConfigDefaults.prepare(game);
      JsonObject expected = old.deepCopy();
      for (String id : ModernFeatureIntegrationTest.FLAGS) {
        int split = id.indexOf('.'); expected.getAsJsonObject("settings").getAsJsonObject(id.substring(0, split))
            .add(id.substring(split + 1), DefaultBases.value(base, id, new JsonPrimitive(false)));
      }
      assertEquals(expected, PowerConfig.document(), base.toString());
      try (var backups = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(backups.anyMatch(dir -> { try { return Files.exists(dir.resolve("complete.json")) && Arrays.equals(original, Files.readAllBytes(dir.resolve("file-0"))); } catch (Exception failure) { return false; } }), "verified original backup for " + base);
      }
      byte[] initialized = Files.readAllBytes(PowerConfig.path()); ConfigDefaults.prepare(game); assertArrayEquals(initialized, Files.readAllBytes(PowerConfig.path()));
      JsonObject switches = PowerConfig.document().getAsJsonObject("settings").deepCopy();
      DefaultBases.use(base == DefaultBases.Base.BETA ? DefaultBases.Base.POWER_BETA : DefaultBases.Base.BETA, game); ConfigDefaults.prepare(game);
      JsonObject after = PowerConfig.document().getAsJsonObject("settings");
      for (String id : ModernFeatureIntegrationTest.FLAGS) { int split = id.indexOf('.'); assertEquals(switches.getAsJsonObject(id.substring(0, split)).get(id.substring(split + 1)), after.getAsJsonObject(id.substring(0, split)).get(id.substring(split + 1)), id); }
      assertEquals(presets, after.get("savedCreativeHotbars"));
      for (String id : List.of("fov", "guiScale", "gui_scale", "fpsLimit", "framerate_limit")) assertEquals(settings.getAsJsonObject("native").get(id), after.getAsJsonObject("native").get(id), id);
    }
  }
  private static java.lang.reflect.Field field(Class<?> type, String name) throws Exception { var field = type.getDeclaredField(name); field.setAccessible(true); return field; }
}
