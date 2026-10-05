package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicFiltersTest {
  @Test void tabsAndFolderSelectorFitEverySupportedWidth() {
    for (int width = 180; width <= 1600; width++) {
      for (String selector : new String[]{"", "folder", "presets"}) {
        var layout = MusicFilters.layout(31, 111, width, selector);
        var tabs = layout.tabs();
        for (var t : tabs) {
          assertTrue(t.x() >= 31 && t.x() + t.width() <= 31 + width);
          assertTrue(t.y() + 18 <= 111 + layout.height());
          for (var other : tabs) if (t != other && t.y() == other.y())
            assertTrue(t.x() + t.width() < other.x() || other.x() + other.width() < t.x());
          if (!selector.isEmpty() && t.y() == layout.folderY())
            assertTrue(t.x() + t.width() < layout.folderX() || t.x() > layout.folderX()+layout.folderWidth());
        }
        if (!selector.isEmpty()) {
          var active = tabs.stream().filter(t -> t.id().equals(selector)).findFirst().orElseThrow();
          assertEquals(active.y(), layout.folderY());
          assertEquals(active.x()+active.width()+2, layout.folderX());
          assertEquals(java.util.List.of("active", "", "favourites", "presets", "custom", "folder"), tabs.stream().map(MusicFilters.Tab::id).toList());
          assertTrue(layout.folderWidth() >= 80);
          assertTrue(layout.folderX() + layout.folderWidth() <= 31 + width);
        }
      }
    }
  }
}
