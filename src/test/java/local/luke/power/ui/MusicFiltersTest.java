package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicFiltersTest {
  private static final List<String> VIEWS = List.of("library", "playlists", "favorites", "active");

  private static MusicFilters.Layout layout(int width, String selected, String activeName) {
    return MusicFilters.layout(31, 95, width, selected, activeName, label -> label.length() * 6);
  }

  @Test void everyTabStaysAccessibleAndTheSelectedTabIsVisibleAtEverySupportedWidth() {
    for (int width = 180; width <= 1600; width++) for (String selected : VIEWS)
      for (String name : List.of("Beta 1.7.3", "Favorites", "A very long world playlist name")) {
        var layout = layout(width, selected, name);
        assertEquals("library", layout.tabs().get(0).id());
        assertTrue(layout.tabs().stream().anyMatch(tab -> tab.id().equals(selected)));
        var ids = new HashSet<>(layout.tabs().stream().map(MusicFilters.Tab::id).toList());
        layout.hiddenTabs().forEach(tab -> assertTrue(ids.add(tab.id())));
        assertEquals(new HashSet<>(VIEWS), ids);
        int previousEnd = 30;
        for (var tab : layout.tabs()) {
          assertTrue(tab.x() > previousEnd);
          assertEquals(95, tab.y());
          assertTrue(tab.x() + tab.width() <= 31 + width);
          previousEnd = tab.x() + tab.width();
        }
        assertTrue(previousEnd + 4 <= layout.addFolderX());
        assertEquals(95, layout.actionsY());
        assertEquals(18, layout.height());
        if (layout.overflow()) {
          assertEquals(layout.addFolderX(), layout.reloadX());
          assertEquals(31 + width, layout.reloadX() + 20);
        } else {
          assertTrue(layout.hiddenTabs().isEmpty());
          assertEquals(4, layout.reloadX() - layout.addFolderX() - 68);
          assertEquals(31 + width, layout.reloadX() + 48);
        }
      }
  }

  @Test void selectedAndUnselectedTabsUseStableSlots() {
    for (int width : List.of(180, 200, 320, 700)) {
      var library = layout(width, "library", "Beta 1.7.3");
      for (String selected : VIEWS) {
        var other = layout(width, selected, "Beta 1.7.3");
        assertEquals(library.tabs().get(0), other.tabs().get(0));
        assertEquals(library.tabs().stream().map(tab -> List.of(tab.x(), tab.width())).toList(),
            other.tabs().stream().map(tab -> List.of(tab.x(), tab.width())).toList());
        assertEquals(library.addFolderX(), other.addFolderX());
      }
    }
  }

  @Test void narrowViewsKeepOneRowAndAnOverflowSlotForTabsAndActions() {
    for (String selected : VIEWS) {
      var layout = layout(200, selected, "Beta 1.7.3");
      assertEquals(2, layout.tabs().size());
      assertEquals(2, layout.hiddenTabs().size());
      assertTrue(layout.overflow());
      assertEquals(18, layout.height());
      assertTrue(173 - (95 + layout.height() + 10) >= 20 + 24);
    }
  }

  @Test void wideViewsShowAllFourTabsAndNormalActions() {
    for (String selected : VIEWS) {
      var layout = layout(600, selected, "World playlist");
      assertEquals(VIEWS, layout.tabs().stream().map(MusicFilters.Tab::id).toList());
      assertFalse(layout.overflow());
      assertTrue(layout.hiddenTabs().isEmpty());
      assertEquals("World playlist", layout.tabs().get(3).label());
    }
  }
}
