package local.luke.power.building;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import local.luke.power.building.config.Settings;
import local.luke.power.building.config.SettingsValidator;
import org.junit.jupiter.api.Test;

class ObsidianMiningTest {
  private static final float VANILLA = 1.0f / 10 / 30;

  @Test void vanillaAndModernEndpointsUseTheExpectedNumberOfTicks() {
    assertEquals(VANILLA, ObsidianMining.progress(VANILLA, 0));
    assertEquals(301, ticks(VANILLA));
    assertEquals(31, ticks(ObsidianMining.progress(VANILLA, 100)));
    assertEquals(49.0f / 50 / 30, ObsidianMining.progress(VANILLA, 100), 1e-8f);
  }

  @Test void sliderIsMonotonicAndPreservesMiningPenalties() {
    float previous = VANILLA;
    for (int speed = 1; speed <= 100; speed++) {
      float progress = ObsidianMining.progress(VANILLA, speed);
      assertTrue(progress > previous);
      assertEquals(progress / 5, ObsidianMining.progress(VANILLA / 5, speed), 1e-8f);
      assertEquals(progress / 25, ObsidianMining.progress(VANILLA / 25, speed), 1e-8f);
      assertEquals(0, ObsidianMining.progress(0, speed));
      previous = progress;
    }
  }

  @Test void settingsDefaultOffAndSurviveCopyAndGson289() {
    var gson = new Gson();
    assertEquals(0, new Settings().obsidianBreakingSpeed);
    assertEquals(0, gson.fromJson("{}", Settings.class).obsidianBreakingSpeed);
    var settings = new Settings();
    for (int speed : new int[] {0, 1, 50, 100}) {
      settings.obsidianBreakingSpeed = speed;
      SettingsValidator.validate(settings);
      assertEquals(speed, settings.copy().obsidianBreakingSpeed);
      var loaded = gson.fromJson(gson.toJson(settings), Settings.class);
      SettingsValidator.validate(loaded);
      assertEquals(speed, loaded.obsidianBreakingSpeed);
    }
    for (int speed : new int[] {-1, 101, Integer.MAX_VALUE}) {
      settings.obsidianBreakingSpeed = speed;
      assertThrows(IllegalArgumentException.class, () -> SettingsValidator.validate(settings));
    }
  }

  private static int ticks(float progress) {
    float damage = 0;
    int ticks = 0;
    while (damage < 1 && ticks < 10000) { damage += progress; ticks++; }
    return ticks;
  }
}
