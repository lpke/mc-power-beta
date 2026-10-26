package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AudioToolbarTest {
  @Test void buttonsFitBothLayoutsWithoutOverlap() {
    for (int width = 200; width <= 1600; width++)
      for (int queued : new int[]{0, 256}) {
        var layout = AudioToolbar.layout(37, width, queued, true, false);
        var buttons = layout.buttons();
        for (int i = 0; i < buttons.size(); i++) {
          var button = buttons.get(i);
          assertTrue(button.x() >= 37 && button.x() + button.width() <= 37 + width);
          assertTrue(button.y() + 18 <= layout.bottom());
          for (int j = i + 1; j < buttons.size(); j++) {
            var other = buttons.get(j);
            if (button.y() == other.y())
              assertTrue(other.x() > button.x() + button.width(), "width " + width);
          }
        }
        assertEquals(queued > 0, buttons.stream().anyMatch(b -> b.action() == AudioToolbar.Action.QUEUE));
        var last = buttons.get(buttons.size() - 1);
        assertEquals(AudioToolbar.Action.SHUFFLE, last.action());
        assertEquals(37 + width, last.x() + last.width());
        assertEquals(layout, AudioToolbar.editor(37, width, queued, true));
      }
  }

  @Test void playbackPositionsDoNotMoveWhenQueueOrEditorChanges() {
    for (int width : new int[]{200, 244, 342, 380, 465, 640}) {
      var playback = AudioToolbar.playback(37, width, true);
      for (int queued : new int[]{0, 256}) {
        var main = AudioToolbar.layout(37, width, queued, true, false);
        var editor = AudioToolbar.editor(37, width, queued, true);
        for (var button : playback.buttons()) {
          assertEquals(button, main.buttons().stream().filter(b -> b.action() == button.action()).findFirst().orElseThrow());
          assertEquals(button, editor.buttons().stream().filter(b -> b.action() == button.action()).findFirst().orElseThrow());
          assertEquals(52, button.y());
        }
      }
    }
  }
}
