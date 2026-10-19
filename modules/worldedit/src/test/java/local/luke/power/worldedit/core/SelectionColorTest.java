package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import local.luke.power.ui.ColorValue;
import local.luke.power.worldedit.config.Settings;
import local.luke.power.worldedit.config.SettingsValidator;
import org.junit.jupiter.api.Test;

class SelectionColorTest {
  @Test void oldAndCustomColorsSurviveGson289AndCopies() {
    Gson gson = new Gson();
    for (int stored : new int[] {0, 1, 2, 3, ColorValue.CUSTOM, ColorValue.CUSTOM | 1, ColorValue.CUSTOM | 3, 0x1ffffff}) {
      Settings loaded = gson.fromJson("{\"color\":" + stored + ",\"opacity\":30}", Settings.class);
      assertDoesNotThrow(() -> SettingsValidator.validate(loaded));
      Settings restarted = gson.fromJson(gson.toJson(loaded.copy()), Settings.class);
      assertEquals(stored, restarted.color); assertEquals(30, restarted.opacity);
      assertEquals(ColorValue.Palette.WORLD_EDIT.rgb(stored), ColorValue.Palette.WORLD_EDIT.rgb(restarted.color));
    }
    Settings invalid = new Settings(); invalid.color = 4;
    assertThrows(IllegalArgumentException.class, () -> SettingsValidator.validate(invalid));
    invalid.color = 0; invalid.opacity = 101;
    assertThrows(IllegalArgumentException.class, () -> SettingsValidator.validate(invalid));
  }
}
