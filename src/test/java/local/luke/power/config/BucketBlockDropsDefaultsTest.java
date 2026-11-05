package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class BucketBlockDropsDefaultsTest {
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
  @Test void freshDefaultsAndBackedUpExistingPreferencesFollowTheSelectedBaseOnlyOnce() throws Exception {
    ConfigDefaults.prepare(game);
    assertTrue(PowerConfig.section("gameplay").get("bucketBlockDrops").getAsBoolean());
    assertTrue(new local.luke.power.gameplay.GameplaySettings().bucketBlockDrops);
    for (var selected : DefaultBases.Base.values()) {
      boolean enabled = selected != DefaultBases.Base.BETA;
      assertEquals(enabled, DefaultBases.value(selected, "gameplay.bucketBlockDrops", new JsonPrimitive(true)).getAsBoolean());
      JsonObject original = PowerConfig.document();
      original.getAsJsonObject("settings").getAsJsonObject("defaults").addProperty("base", selected.id);
      var gameplay = original.getAsJsonObject("settings").getAsJsonObject("gameplay");
      gameplay.remove("bucketBlockDrops"); gameplay.addProperty("unknownGameplayEvidence", "retained");
      PowerConfig.write(original); byte[] before = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      JsonObject expected = original.deepCopy();
      expected.getAsJsonObject("settings").getAsJsonObject("gameplay").addProperty("bucketBlockDrops", enabled);
      assertEquals(expected, PowerConfig.document());
      try (var backups = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(backups.anyMatch(dir -> {
          try { return Files.exists(dir.resolve("complete.json")) && Arrays.equals(before, Files.readAllBytes(dir.resolve("file-0"))); }
          catch (Exception failure) { return false; }
        }), "verified original backup for " + selected);
      }
      DefaultBases.use(enabled ? DefaultBases.Base.BETA : DefaultBases.Base.POWER_BETA, game);
      byte[] frozen = Files.readAllBytes(PowerConfig.path()); ConfigDefaults.prepare(game);
      assertArrayEquals(frozen, Files.readAllBytes(PowerConfig.path()));
      assertEquals(enabled, PowerConfig.section("gameplay").get("bucketBlockDrops").getAsBoolean());
      var explicit = PowerConfig.section("gameplay"); explicit.addProperty("bucketBlockDrops", !enabled); PowerConfig.put("gameplay", explicit);
      ConfigDefaults.prepare(game); assertEquals(explicit, PowerConfig.section("gameplay"));
    }
  }
  @Test void powerBetaCameraDefaultsRemainIndependentOfOtherBasesAndSavedPreferences() throws Exception {
    ConfigDefaults.prepare(game);
    assertTrue(PowerConfig.section("gameplay").get("modernCrawling").getAsBoolean());
    assertTrue(PowerConfig.section("gameplay").get("crouchCameraHeight").getAsBoolean());
    assertFalse(PowerConfig.section("gameplay").get("lowerCrouchCamera").getAsBoolean());
    for (var selected : DefaultBases.Base.values()) {
      assertEquals(selected == DefaultBases.Base.POWER_BETA,
          DefaultBases.value(selected, "gameplay.modernCrawling", new JsonPrimitive(false)).getAsBoolean());
      assertEquals(selected == DefaultBases.Base.POWER_BETA,
          DefaultBases.value(selected, "gameplay.crouchCameraHeight", new JsonPrimitive(false)).getAsBoolean());
      assertEquals(selected != DefaultBases.Base.POWER_BETA,
          DefaultBases.value(selected, "gameplay.lowerCrouchCamera", new JsonPrimitive(true)).getAsBoolean());
    }
    var original = PowerConfig.section("gameplay");
    original.addProperty("modernCrawling", false); original.addProperty("lowerCrouchCamera", true);
    for (boolean crouching : new boolean[]{false, true}) {
      original.addProperty("crouchCameraHeight", crouching);
      PowerConfig.put("gameplay", original); byte[] before = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game); assertArrayEquals(before, Files.readAllBytes(PowerConfig.path()));
    }
  }
  private static java.lang.reflect.Field field(Class<?> type, String name) throws Exception { var f = type.getDeclaredField(name); f.setAccessible(true); return f; }
}
