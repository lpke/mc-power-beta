package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MusicRowLayoutTest {
  @Test
  void actionsAndVolumesDoNotOverlapAtSupportedSizes() {
    for (int width = 180; width <= 1600; width++)
      for (boolean queue : new boolean[] {false, true}) {
        var l = MusicRowLayout.at(30, width, queue);
        assertTrue(l.volumeX() >= 30);
        assertTrue(l.volumeWidth() >= 36);
        assertEquals(4, l.favouriteX() - l.volumeX() - l.volumeWidth());
        assertTrue(l.queueX() + l.buttonWidth() <= 30 + width);
        assertTrue(l.nameWidth() > 0);
        if (l.height() == 24) assertTrue(l.nameX() + l.nameWidth() < l.volumeX());
        if (queue) {
          assertTrue(l.upX() < l.downX());
          assertTrue(l.downX() < l.playX());
        }
      }
  }
}
