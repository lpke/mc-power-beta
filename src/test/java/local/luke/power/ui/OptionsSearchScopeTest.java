package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.google.gson.JsonPrimitive;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;

class OptionsSearchScopeTest {
  @Test void allTabsShareOneChoiceAndControlsAlwaysSearchControls() {
    OptionsSearchScope.reset();
    try {
      assertFalse(OptionsSearchScope.thisTab("General"));
      assertFalse(OptionsSearchScope.thisTab("Audio"));
      assertTrue(OptionsSearchScope.thisTab("Controls"));
      assertFalse(OptionsSearchScope.thisTab("General"));
      OptionsSearchScope.toggle("General");
      for (String page : List.of("General", "Controls", "Audio")) assertTrue(OptionsSearchScope.thisTab(page));
      OptionsSearchScope.toggle("Audio");
      for (String page : List.of("General", "Audio", "Video")) assertFalse(OptionsSearchScope.thisTab(page));
      assertTrue(OptionsSearchScope.thisTab("Controls"));
    } finally { OptionsSearchScope.reset(); }
  }
  private Setting setting(String id, String page, String group) {
    return new Setting(id, "test", page, group, id, "", Setting.Kind.INTEGER,
        new JsonPrimitive(1), new JsonPrimitive(1), 0, 100, 1, List.of(), false);
  }
  @Test void minimapSearchTracksTheAvailableEditorAndOtherHudSettingsKeepTheirRealRoute() {
    var size = setting("maps.size", "Interface", "Minimap");
    assertEquals("Map", OptionsSearchScope.page(size, true));
    assertEquals("Map / Minimap appearance", OptionsSearchScope.group(size, 1, true));
    assertEquals("General", OptionsSearchScope.page(size, false));
    assertEquals("General / Interface / HUD editor / Minimap", OptionsSearchScope.group(size, 1, false));
    var debug = setting("interface.debugAxes", "Interface", "Debug (F3) info");
    assertEquals("General", OptionsSearchScope.page(debug, true));
    assertEquals("General / Interface / HUD editor / F3 information", OptionsSearchScope.group(debug, 1, true));
    var range = setting("maps.radarRange", "Map", "Entity radar");
    assertEquals("Map / Entity radar", OptionsSearchScope.group(range, 1, true));
  }
}
