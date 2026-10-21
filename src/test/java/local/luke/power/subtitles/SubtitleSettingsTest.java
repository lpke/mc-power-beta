package local.luke.power.subtitles;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class SubtitleSettingsTest {
  @Test void gson289RoundTripKeepsAllPreferencesAndStableSharedFallback() {
    var settings = new SubtitleSettings(); assertFalse(settings.enabled);
    settings.enabled = true; settings.position = 2; settings.offsetX = 100; settings.offsetY = -19;
    settings.textSize = 1.4; settings.backgroundOpacity = 23; settings.displaySeconds = 5.2; settings.hideWithDebug = true; settings.showInInventory = false;
    Gson gson = new Gson(); var copy = gson.fromJson(gson.toJson(settings), SubtitleSettings.class); copy.validate();
    assertEquals(gson.toJson(settings), gson.toJson(copy));
  }
  @Test void invalidDurationsScalesAnchorsAndOffsetsCannotPublish() {
    var settings = new SubtitleSettings(); settings.displaySeconds = Double.NaN; assertThrows(IllegalArgumentException.class, settings::validate);
    settings.displaySeconds = 3; settings.textSize = 0; assertThrows(IllegalArgumentException.class, settings::validate);
    settings.textSize = 1; settings.position = 9; assertThrows(IllegalArgumentException.class, settings::validate);
    settings.position = 8; settings.offsetX = Integer.MIN_VALUE; assertThrows(IllegalArgumentException.class, settings::validate);
  }
}
