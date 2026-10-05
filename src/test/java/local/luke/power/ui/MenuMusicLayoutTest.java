package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.audio.AudioSettings.MenuControlsPosition;
import org.junit.jupiter.api.Test;

class MenuMusicLayoutTest {
  @Test void optionalScrubberFitsEveryAnchorWithoutChangingPanelWidth() {
    for (int[] size : new int[][] {{320, 240}, {427, 240}, {854, 480}})
      for (var anchor : MenuControlsPosition.values()) {
        var box = MenuMusicLayout.at(size[0], size[1], 100, 180, anchor, 0, 0, true);
        assertEquals(48, box.height());
        assertTrue(box.width() <= 200);
        assertTrue(box.y() + box.height() <= size[1] - 4);
      }
  }
  @Test
  void everyAnchorRemainsReachableAfterResizeAndLargeOffsets() {
    for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 420}, {854, 480}, {1920, 480}})
      for (var position : MenuControlsPosition.values())
        for (int offset : new int[] {-4096, 0, 4096}) {
          var box = MenuMusicLayout.at(size[0], size[1], 100, 200, position, offset, offset);
          assertTrue(box.x() >= 4 && box.y() >= 4);
          assertTrue(box.x() + box.width() <= size[0] - 4);
          assertTrue(box.y() + box.height() <= size[1] - 4);
        }
  }

  @Test
  void middleEdgesLeaveRoomForTheMainMenuButtons() {
    for (int width : new int[] {640, 854, 1920}) {
      var left = MenuMusicLayout.at(width, 480, 160, 300, MenuControlsPosition.MIDDLE_LEFT, 0, 0);
      var right = MenuMusicLayout.at(width, 480, 160, 300, MenuControlsPosition.MIDDLE_RIGHT, 0, 0);
      assertTrue(left.x() + left.width() < width / 2 - 100);
      assertTrue(right.x() > width / 2 + 100);
    }
  }

  @Test
  void menuAnchorsFollowActualButtonPositionsAndOffsetsUseGuiPixels() {
    var above = MenuMusicLayout.at(854, 480, 180, 320, MenuControlsPosition.MENU_TOP, 0, 0);
    assertEquals(180 - 8, above.y() + above.height());
    var below = MenuMusicLayout.at(854, 480, 180, 320, MenuControlsPosition.MENU_BOTTOM, 13, -7);
    assertEquals(321, below.y());
    assertEquals(340, below.x());
  }
}
