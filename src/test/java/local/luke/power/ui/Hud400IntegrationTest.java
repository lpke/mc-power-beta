package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;

class Hud400IntegrationTest {
  @Test void allNineSubtitleControlsShareOneEditorAndPositionResetExcludesContent() {
    for (String field : List.of("enabled", "position", "offsetX", "offsetY", "textSize", "backgroundOpacity", "displaySeconds", "hideWithDebug", "showInInventory")) {
      Setting row = row("subtitles." + field, "Subtitles");
      assertTrue(HudEditorSettings.contains(row), field);
      for (int mode : new int[]{0, 1, 2}) assertEquals("subtitles", HudEditorSettings.tab(row, mode), field);
      assertEquals(Set.of("position", "offsetX", "offsetY").contains(field), HudEditorSections.position(row), field);
    }
    assertEquals("Subtitles", HudEditorSettings.tabLabel("subtitles"));
  }
  @Test void offhandSharesNativeVisibilityAndPositionsWithoutMovingOtherHudElements() {
    String prefix = "power_hud:hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.OFFHAND_POSITION_CONFIG.";
    for (String field : List.of("enableVisibility", "horizontalPosition", "horizontalPositionOffset", "verticalPosition", "verticalPositionOffset")) {
      Setting row = row(prefix + field, "Offhand position"); assertTrue(HudEditorSettings.contains(row), field);
      assertEquals("combined", HudEditorSettings.tab(row, 1), field); assertEquals("native.6", HudEditorSettings.tab(row, 2), field);
      assertEquals(!field.equals("enableVisibility"), HudEditorSections.position(row), field);
    }
    assertEquals("Offhand", HudEditorSettings.tabLabel("native.6"));
    assertEquals("Hotbar", HudEditorSettings.tabLabel("native.0"));
    assertEquals("Stamina", HudEditorSettings.tabLabel("native.5"));
  }
  private static Setting row(String id, String group) {
    return new Setting(id, "fixture", "Interface", group, id, "", Setting.Kind.INTEGER,
        new JsonPrimitive(0), new JsonPrimitive(0), -4096, 4096, 1, List.of(), false);
  }
}
