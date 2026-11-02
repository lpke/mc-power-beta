package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicFooterLayoutTest {
  @Test void compactPlaylistFooterMovesPreferenceAboveSaveAndCancel() {
    var footer = MusicFooterLayout.at(8, 134, 212, 82, 58);
    assertEquals(189, footer.y());
    assertEquals(96, footer.buttonX());
    assertTrue(footer.buttonX() + footer.buttonWidth() < 320);
  }

  @Test void wideFooterKeepsPreferenceLeftOfActions() {
    var footer = MusicFooterLayout.at(8, 300, 212, 82, 58);
    assertEquals(212, footer.y());
    assertTrue(footer.buttonX() + footer.buttonWidth() + 6 <= 300);
  }

  @Test void compactPlaylistActionsCanKeepAllControlsOnOneLine() {
    var footer = MusicFooterLayout.at(8, 172, 212, 78, 56);
    assertEquals(212, footer.y());
    assertTrue(footer.buttonX() + footer.buttonWidth() + 6 <= 172);
  }
}
