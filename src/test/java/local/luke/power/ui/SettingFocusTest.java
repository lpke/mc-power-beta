package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SettingFocusTest {
  @Test void linkedRowsKeepContextAndStayBelowStickyHeadings() {
    for (int height : new int[] {24, 40}) for (int viewport : new int[] {100, 180, 400}) {
      double scroll = SettingFocus.scroll(500, height, viewport, 1400, SettingsGroup.HEIGHT);
      assertTrue(500 - scroll >= SettingsGroup.HEIGHT);
      assertTrue(500 + height - scroll <= viewport);
      assertTrue(500 - scroll >= Math.min(height, (viewport - height) / 2), "A neighboring row remains visible above the target");
    }
  }
  @Test void firstAndLastRowsRespectContentBounds() {
    assertEquals(0, SettingFocus.scroll(24, 24, 400, 800, 24));
    assertEquals(400, SettingFocus.scroll(776, 24, 400, 800, 24));
    assertEquals(0, SettingFocus.scroll(64, 40, 180, 104, 24));
  }
}
