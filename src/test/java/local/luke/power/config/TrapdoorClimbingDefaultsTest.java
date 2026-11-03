package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class TrapdoorClimbingDefaultsTest {
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
    assertTrue(PowerConfig.section("gameplay").get("modernTrapdoorClimbing").getAsBoolean());
    assertFalse(new local.luke.power.gameplay.GameplaySettings().modernTrapdoorClimbing);
    for (var selected : DefaultBases.Base.values()) {
      boolean enabled = selected == DefaultBases.Base.POWER_BETA;
      assertEquals(enabled, DefaultBases.value(selected, "gameplay.modernTrapdoorClimbing", new JsonPrimitive(false)).getAsBoolean());
      JsonObject original = PowerConfig.document();
      original.getAsJsonObject("settings").getAsJsonObject("defaults").addProperty("base", selected.id);
      var gameplay = original.getAsJsonObject("settings").getAsJsonObject("gameplay");
      gameplay.remove("modernTrapdoorClimbing"); gameplay.addProperty("unknownGameplayEvidence", "retained");
      PowerConfig.write(original); byte[] before = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      JsonObject expected = original.deepCopy();
      expected.getAsJsonObject("settings").getAsJsonObject("gameplay").addProperty("modernTrapdoorClimbing", enabled);
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
      assertEquals(enabled, PowerConfig.section("gameplay").get("modernTrapdoorClimbing").getAsBoolean());
      var explicit = PowerConfig.section("gameplay"); explicit.addProperty("modernTrapdoorClimbing", !enabled); PowerConfig.put("gameplay", explicit);
      ConfigDefaults.prepare(game); assertEquals(explicit, PowerConfig.section("gameplay"));
    }
  }
  private static java.lang.reflect.Field field(Class<?> type, String name) throws Exception { var f = type.getDeclaredField(name); f.setAccessible(true); return f; }
}
