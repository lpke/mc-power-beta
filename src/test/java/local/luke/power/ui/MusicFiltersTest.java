package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicFiltersTest {
  @Test void tabsAndFolderSelectorFitEverySupportedWidth() {
    for (int width = 180; width <= 1600; width++) {
      for (boolean folder : new boolean[]{false, true}) {
        var layout = MusicFilters.layout(31, 111, width, folder);
        var tabs = layout.tabs();
        for (var t : tabs) {
          assertTrue(t.x() >= 31 && t.x() + t.width() <= 31 + width);
          assertTrue(t.y() + 18 <= 111 + layout.height());
          for (var other : tabs) if (t != other && t.y() == other.y())
            assertTrue(t.x() + t.width() < other.x() || other.x() + other.width() < t.x());
          if (folder && t.y() == layout.folderY()) assertTrue(t.x() + t.width() < layout.folderX());
        }
        if (folder) {
          assertTrue(layout.folderWidth() >= 80);
          assertTrue(layout.folderX() + layout.folderWidth() <= 31 + width);
        }
      }
    }
  }
}
