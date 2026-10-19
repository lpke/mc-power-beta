package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorSectionsTest {
  private Setting setting(String id) {
    return new Setting(id, "test", "Interface", "", id, "", Setting.Kind.INTEGER,
        new JsonPrimitive(0), new JsonPrimitive(0), -4096, 4096, 1, List.of(), false);
  }
  @Test void positionResetIncludesAnchorsAndProviderOffsetsButNeverModeVisibilityOrContent() {
    for (String id : List.of("interface.debugOrder", "interface.debugRightOrder", "infoHud.clusters.0.position", "infoHud.clusters.3.offsetX", "activeTweaks.offsetY",
        "power_hud:hudpositions.SIMPLE_HUD_POSITION_CONFIG.horizontalPosition",
        "power_hud:hudpositions.ADVANCED_HUD_POSITIONS_CONFIG.HEARTS_POSITION_CONFIG.verticalPositionOffset",
        "power_hud:hudpositions.putStatusBarIconsBelowHotbar", "power_compat:config.overlayAdditionsYOffset",
        "entityculling:config.f3InfoYOffset", "power_environment:config.MUSIC_CONFIG.overlayForMusicInDebugYOffset"))
      assertTrue(HudEditorSections.position(setting(id)), id);
    for (String id : List.of(HudEditorSettings.ENTRY, "infoHud.enabled", "infoHud.selectedCluster",
        "infoHud.clusters.0.enabled", "infoHud.clusters.0.coordinates", "infoHud.clusters.0.hideWithDebug",
        "infoHud.clusters.0.textSize", "infoHud.clusters.0.opacity", "activeTweaks.fastPlacement",
        "power_hud:hudpositions.SIMPLE_HUD_POSITION_CONFIG.enableVisibility", "interface.useFeetYCoordinates",
        "entityculling:config.showF3Info", "power_environment:config.MUSIC_CONFIG.overlayForMusicInDebug",
        "unrelated.offsetX")) assertFalse(HudEditorSections.position(setting(id)), id);
  }
  @Test void groupingKeepsStableOrderWithinSectionsAndOffsetsBesideTheirProvider() {
    var content = setting("infoHud.clusters.0.fps"); var x = setting("infoHud.clusters.0.offsetX");
    var visible = setting("infoHud.clusters.0.enabled"); var y = setting("infoHud.clusters.0.offsetY");
    var color = setting("infoHud.clusters.0.textColor");
    assertEquals(List.of(visible, x, y, color, content), HudEditorSections.order(List.of(content, x, visible, y, color)));
    assertEquals("Culling statistics", HudEditorSections.group(setting("entityculling:config.f3InfoYOffset")));
    assertEquals("Music", HudEditorSections.group(setting("power_environment:config.MUSIC_CONFIG.overlayForMusicInDebugYOffset")));
  }
}
