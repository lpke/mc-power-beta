package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.config.Catalog;
import org.junit.jupiter.api.Test;

class MusicFadeSettingsTest {
  @Test void missingFadeDefaultsToOneSecondAndSavedZeroAndPreciseValuesRemainSupported() {
    assertEquals(1000, new AudioSettings().musicFadeMillis);
    assertEquals(1000, Catalog.JSON.fromJson("{}", AudioSettings.class).musicFadeMillis);
    for (int fade : new int[]{0, 1, 137, 5000, 5001, 9999, 10000}) {
      AudioSettings settings = Catalog.JSON.fromJson("{\"musicFadeMillis\":" + fade + "}", AudioSettings.class);
      assertDoesNotThrow(settings::validate); assertEquals(fade, settings.musicFadeMillis);
    }
    for (int fade : new int[]{-1, 10001}) {
      AudioSettings settings = new AudioSettings(); settings.musicFadeMillis = fade;
      assertThrows(IllegalArgumentException.class, settings::validate);
    }
  }
  @Test void tenSecondFadesKeepTheExpectedOpeningAndEndingGain() {
    assertEquals(.1f, MusicFade.gain(10000, 1, 60));
    assertEquals(.5f, MusicFade.gain(10000, 5, 60));
    assertEquals(1, MusicFade.gain(10000, 10, 60));
    assertEquals(.5f, MusicFade.gain(10000, 55, 60));
    assertEquals(0, MusicFade.gain(10000, 60, 60));
  }
}
