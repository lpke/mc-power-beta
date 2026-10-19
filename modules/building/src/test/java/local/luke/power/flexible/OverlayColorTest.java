package local.luke.power.flexible;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import local.luke.power.flexible.config.Settings;
import local.luke.power.flexible.config.SettingsValidator;
import local.luke.power.ui.ColorValue;
import org.junit.jupiter.api.Test;

class OverlayColorTest {
  @Test void oldAndCustomColorsSurviveGson289AndCopies() {
    Gson gson = new Gson();
    for (int stored : new int[] {0, 1, 2, 3, ColorValue.CUSTOM, ColorValue.CUSTOM | 1, ColorValue.CUSTOM | 3, 0x1ffffff}) {
      Settings loaded = gson.fromJson("{\"overlayColor\":" + stored + ",\"overlayOpacity\":30}", Settings.class);
      assertDoesNotThrow(() -> SettingsValidator.validate(loaded));
      Settings restarted = gson.fromJson(gson.toJson(loaded.copy()), Settings.class);
      assertEquals(stored, restarted.overlayColor); assertEquals(30, restarted.overlayOpacity);
      assertEquals(ColorValue.Palette.FLEXIBLE.rgb(stored), ColorValue.Palette.FLEXIBLE.rgb(restarted.overlayColor));
    }
    Settings invalid = new Settings(); invalid.overlayColor = 4;
    assertThrows(IllegalArgumentException.class, () -> SettingsValidator.validate(invalid));
    invalid.overlayColor = 0; invalid.overlayOpacity = 100;
    assertThrows(IllegalArgumentException.class, () -> SettingsValidator.validate(invalid));
  }
}
