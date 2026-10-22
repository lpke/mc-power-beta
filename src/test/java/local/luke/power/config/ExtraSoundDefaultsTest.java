package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.AudioSettings;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExtraSoundDefaultsTest {
  private static final List<String> KEYS = List.of("seedPlantingSounds", "hoeTillingSounds", "bucketSounds", "boneMealSounds",
      "lavaPopSounds", "waterSplashSounds", "swimmingSounds");
  @TempDir Path game;
  private Object priorFile, priorBase, priorSetup;

  @org.junit.jupiter.api.BeforeEach void isolateProcessState() throws Exception {
    PowerConfig.flushPending();
    var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); priorFile = file.get(null); file.set(null, null);
    var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); priorBase = base.get(null);
    var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); priorSetup = setup.get(null);
  }
  @org.junit.jupiter.api.AfterEach void restoreProcessState() throws Exception {
    try { PowerConfig.flushPending(); }
    finally {
      var file = PowerConfig.class.getDeclaredField("file"); file.setAccessible(true); file.set(null, priorFile);
      var base = DefaultBases.class.getDeclaredField("current"); base.setAccessible(true); base.set(null, priorBase);
      var setup = DefaultBases.class.getDeclaredField("setupComplete"); setup.setAccessible(true); setup.set(null, priorSetup);
    }
  }

  @Test void missingSoundsUseTheSelectedBaseOnceAfterAVerifiedBackup() throws Exception {
    ConfigDefaults.prepare(game);
    for (var base : DefaultBases.Base.values()) {
      JsonObject old = PowerConfig.document(); var settings = old.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base", base.id);
      var audio = settings.getAsJsonObject("audio");
      KEYS.forEach(audio::remove); audio.addProperty("unknownAudioEvidence", "retained");
      PowerConfig.write(old); byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      boolean enabled = base == DefaultBases.Base.POWER_BETA;
      JsonObject expected = old.deepCopy();
      for (String key : KEYS) expected.getAsJsonObject("settings").getAsJsonObject("audio").addProperty(key, enabled);
      assertEquals(expected, PowerConfig.document());
      try (var dirs = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(dirs.anyMatch(dir -> {
          try { return Arrays.equals(original, Files.readAllBytes(dir.resolve("file-0"))) && Files.exists(dir.resolve("complete.json")); }
          catch (Exception e) { return false; }
        }));
      }
      DefaultBases.use(enabled ? DefaultBases.Base.BETA : DefaultBases.Base.POWER_BETA, game);
      byte[] frozen = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      assertArrayEquals(frozen, Files.readAllBytes(PowerConfig.path()));
      for (String key : KEYS) assertEquals(enabled, PowerConfig.section("audio").get(key).getAsBoolean());
      try (var transaction = FileTransaction.begin(game, List.of(PowerConfig.path()))) {
        var draft = PowerConfig.section("audio");
        KEYS.forEach(key -> draft.addProperty(key, !enabled)); PowerConfig.put("audio", draft);
      }
      ConfigDefaults.prepare(game);
      assertArrayEquals(frozen, Files.readAllBytes(PowerConfig.path()));
      PowerConfig.read("audio", AudioSettings.class).validate();
    }
  }

  @Test void partialUpgradeKeepsEachExplicitChoiceAndInterruptedWritesRecover() throws Exception {
    ConfigDefaults.prepare(game);
    JsonObject old = PowerConfig.document(); var settings = old.getAsJsonObject("settings");
    settings.getAsJsonObject("defaults").addProperty("base", DefaultBases.Base.POWER_BETA.id);
    var audio = settings.getAsJsonObject("audio");
    audio.addProperty("seedPlantingSounds", false); audio.addProperty("bucketSounds", true);
    audio.remove("hoeTillingSounds"); audio.remove("boneMealSounds");
    PowerConfig.write(old); byte[] before = Files.readAllBytes(PowerConfig.path());
    FileTransaction.begin(game, List.of(PowerConfig.path()));
    Files.writeString(PowerConfig.path(), "interrupted");
    FileTransaction.recover(game);
    assertArrayEquals(before, Files.readAllBytes(PowerConfig.path()));
    ConfigDefaults.prepare(game);
    var restored = PowerConfig.read("audio", AudioSettings.class);
    assertFalse(restored.seedPlantingSounds);
    assertTrue(restored.bucketSounds && restored.hoeTillingSounds && restored.boneMealSounds);
  }

  @Test void missingPauseToolsFreezeTheSelectedBaseAndKeepExplicitChoices() throws Exception {
    ConfigDefaults.prepare(game);
    for (var base : DefaultBases.Base.values()) {
      var old = PowerConfig.document();
      var settings = old.getAsJsonObject("settings");
      settings.getAsJsonObject("defaults").addProperty("base", base.id);
      var menu = settings.getAsJsonObject("interface");
      menu.remove("pauseMapButton"); menu.remove("pauseWaypointsButton");
      PowerConfig.write(old);
      byte[] original = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      boolean enabled = base == DefaultBases.Base.POWER_BETA;
      assertEquals(enabled, PowerConfig.section("interface").get("pauseMapButton").getAsBoolean());
      assertEquals(enabled, PowerConfig.section("interface").get("pauseWaypointsButton").getAsBoolean());
      try (var dirs = Files.list(game.resolve("power-beta-data/settings-backups"))) {
        assertTrue(dirs.anyMatch(dir -> {
          try { return Arrays.equals(original, Files.readAllBytes(dir.resolve("file-0"))) && Files.exists(dir.resolve("complete.json")); }
          catch (Exception e) { return false; }
        }));
      }
      DefaultBases.use(enabled ? DefaultBases.Base.BETA : DefaultBases.Base.POWER_BETA, game);
      byte[] frozen = Files.readAllBytes(PowerConfig.path());
      ConfigDefaults.prepare(game);
      assertArrayEquals(frozen, Files.readAllBytes(PowerConfig.path()));
      var explicit = PowerConfig.section("interface");
      explicit.addProperty("pauseMapButton", !enabled); PowerConfig.put("interface", explicit);
      ConfigDefaults.prepare(game);
      assertEquals(!enabled, PowerConfig.section("interface").get("pauseMapButton").getAsBoolean());
    }
  }
}
