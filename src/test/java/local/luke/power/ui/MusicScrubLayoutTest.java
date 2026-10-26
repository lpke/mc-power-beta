package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.audio.AudioSettings.MenuControlsPosition;
import org.junit.jupiter.api.Test;

class MusicScrubLayoutTest {
  @Test void menuTracksShareTheMenuCentreAndLeaveRoomForTheTime() {
    for (int width : new int[] {320, 427, 854, 1920})
      for (var anchor : new MenuControlsPosition[] {MenuControlsPosition.MENU_TOP, MenuControlsPosition.MENU_BOTTOM})
        for (int offset : new int[] {-11, 0, 13}) {
          var menu = MenuMusicLayout.at(width, 480, 180, 320, anchor, offset, 0, true);
          var bar = MusicScrubLayout.at(menu.x(), menu.width(), false);
          assertEquals(menu.x() * 2 + menu.width(), bar.x() * 2 + bar.width());
          assertTrue(Math.abs(width / 2.0 + offset - (bar.x() + bar.width() / 2.0)) <= .5);
          assertEquals(40, bar.x() - menu.x());
          assertEquals(40, menu.x() + menu.width() - bar.x() - bar.width());
        }
  }

  @Test void thumbKeepsItsWidthInsideBothTrackEndsAndPointerMappingMatches() {
    for (boolean label : new boolean[] {false, true})
      for (int width : new int[] {180, 200, 320, 640}) {
        var bar = MusicScrubLayout.at(37, width, label);
        assertEquals(bar.x(), bar.thumbLeft(0));
        assertEquals(bar.x() + bar.width() - MusicScrubLayout.THUMB_WIDTH, bar.thumbLeft(1));
        for (double fraction : new double[] {0, .25, .5, .75, 1}) {
          int thumb = bar.thumbLeft(fraction);
          assertTrue(thumb >= bar.x());
          assertTrue(thumb + MusicScrubLayout.THUMB_WIDTH <= bar.x() + bar.width());
          assertEquals(fraction, bar.fraction(thumb + MusicScrubLayout.THUMB_WIDTH / 2),
              1.0 / (bar.width() - MusicScrubLayout.THUMB_WIDTH));
        }
      }
  }

  @Test void labelledAudioRowsRetainTheirRightAlignedTrackAndTime() {
    for (int width : new int[] {180, 320, 640}) {
      var bar = MusicScrubLayout.at(37, width, true);
      assertEquals(37 + width - 40, bar.x() + bar.width());
      assertTrue(bar.width() >= 50);
    }
  }
}
