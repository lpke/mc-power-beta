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
        assertEquals(2, l.playX() - l.favouriteX() - l.buttonWidth());
        assertEquals(30 + width, l.playX() + l.buttonWidth());
        assertEquals(4, l.nameX() - MusicRowLayout.hoverLeft(30));
        assertEquals(4, MusicRowLayout.hoverRight(30 + width) - l.playX() - l.buttonWidth());
        assertTrue(l.nameWidth() > 0);
        if (l.height() == 24) assertTrue(l.nameX() + l.nameWidth() < l.volumeX());
        assertEquals(-1, l.previewX());
        assertEquals(-1, l.queueX());
        assertEquals(-1, l.upX());
        assertEquals(-1, l.downX());
      }
  }
}
