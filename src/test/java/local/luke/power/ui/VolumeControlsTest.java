package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VolumeControlsTest {
  @Test void volumeColumnFitsAvailableSpaceAndKeepsOneDesktopWidth() {
    assertEquals(150, VolumeControls.width(600));
    assertEquals(150, VolumeControls.width(150));
    assertEquals(94, VolumeControls.width(94));
    assertEquals(134, VolumeControls.musicWidth(800));
    assertEquals(150, VolumeControls.musicGroupWidth(800));
    for (int span = 180; span <= 1600; span++) {
      var song = MusicRowLayout.at(12, span, false);
      var queue = MusicRowLayout.at(12, span, true);
      assertEquals(song.volumeX(), queue.volumeX());
      assertEquals(song.volumeWidth(), queue.volumeWidth());
      assertTrue(song.volumeWidth() >= 54 && song.volumeWidth() <= 134);
      assertEquals(16, VolumeControls.musicGroupWidth(span) - song.volumeWidth());
      assertTrue(song.nameWidth() >= 12);
      assertEquals(12 + span, song.playX() + song.buttonWidth());
    }
  }
}
