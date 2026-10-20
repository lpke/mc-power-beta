package local.luke.power.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MapSettingsScopeTest {
  @Test void narrowFallbackAndWideMapBothKeepUsableSpace() {
    assertEquals(640, MapSettingsScope.panelWidth(640, 360));
    assertEquals(640, MapSettingsScope.panelWidth(640, 240));
    assertTrue(MapSettingsScope.panelWidth(960, 540) >= 320);
    assertTrue(960 - MapSettingsScope.panelWidth(960, 540) >= 500);
  }
}
