package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicFiltersTest {
  @Test void allThreeEditorTabsRemainVisibleOnOneRow() {
    for (int width = 180; width <= 1600; width++) {
      var layout = MusicFilters.layout(31, 111, width, "");
      assertEquals(List.of("library", "playlists", "settings"), layout.tabs().stream().map(MusicFilters.Tab::id).toList());
      int right = 30;
      for (var tab : layout.tabs()) {
        assertTrue(tab.x() > right);
        assertTrue(tab.x() + tab.width() <= 31 + width);
        assertEquals(111, tab.y());
        right = tab.x() + tab.width();
      }
      assertEquals(18, layout.height());
    }
  }
}
