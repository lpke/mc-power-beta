package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScrollBarTest {
  @Test
  void thumbDragKeepsGrabOffsetAndClampsBothEnds() {
    ScrollBar bar = new ScrollBar();
    var track = new ScrollBar.Track(20, 10, 100, 500, 200);
    assertTrue(bar.press(track, 21, 55)); // thumb begins at 50, grab five pixels below it
    assertEquals(200, bar.drag(track, 55, true));
    assertEquals(300, bar.drag(track, 75, true));
    assertEquals(0, bar.drag(track, -100, true));
    assertEquals(400, bar.drag(track, 900, true));
    assertEquals(200, bar.drag(track, 900, false));
  }

  @Test
  void trackClickCentersThumbAndNonOverflowCannotDrag() {
    ScrollBar bar = new ScrollBar();
    var track = new ScrollBar.Track(20, 10, 100, 500, 0);
    assertTrue(bar.press(track, 21, 60));
    assertEquals(200, bar.drag(track, 60, true));
    assertFalse(bar.press(new ScrollBar.Track(20, 10, 100, 100, 0), 21, 60));
    assertFalse(new ScrollBar.Track(20, 10, 0, 500, 0).visible());
  }
}
