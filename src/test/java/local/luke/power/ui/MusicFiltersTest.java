package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MusicFiltersTest {
  @Test void tabsAndCatalogActionsFitWithoutOverlapAcrossSupportedWidths() {
    for (int width = 180; width <= 1600; width++) for (String view : List.of("library", "favorites")) {
      var layout = MusicFilters.layout(31, 111, width, view);
      assertEquals(List.of("library", "playlists", "favorites"), layout.tabs().stream().map(MusicFilters.Tab::id).toList());
      int right = 30;
      for (var tab : layout.tabs()) {
        assertTrue(tab.x() > right);
        assertTrue(tab.x() + tab.width() <= 31 + width);
        assertEquals(111, tab.y());
        right = tab.x() + tab.width();
      }
      assertTrue(layout.addFolderX() >= 31);
      assertEquals(4, layout.reloadX() - layout.addFolderX() - 68);
      assertEquals(31 + width, layout.reloadX() + 48);
      if ((view.equals("library") ? layout.addFolderX() : layout.reloadX()) - right >= 8) {
        assertEquals(111, layout.actionsY());
        assertEquals(18, layout.height());
      } else {
        assertEquals(137, layout.actionsY());
        assertEquals(44, layout.height());
      }
    }
  }

  @Test void selectingTabsNeverChangesTheirWidthsOrPositions() {
    for (int width : List.of(180, 192, 200, 320, 700)) {
      var tabs = MusicFilters.layout(31, 111, width, "library").tabs();
      assertEquals(tabs, MusicFilters.layout(31, 111, width, "playlists").tabs());
      assertEquals(tabs, MusicFilters.layout(31, 111, width, "favorites").tabs());
      if (width >= 192) assertEquals(List.of(54, 64, 70), tabs.stream().map(MusicFilters.Tab::width).toList());
    }
  }

  @Test void playlistActionsRetainTheirSeparateRow() {
    for (int width : List.of(180, 292, 500))
      assertEquals(18, MusicFilters.layout(31, 111, width, "playlists").height());
  }

  @Test void shortNarrowViewsKeepFullTabLabelsAndAnInlineActionSlot() {
    var measured = Map.of("Library", 34, "Playlists", 40, "Favorites", 46);
    var library = MusicFilters.compact(31, 95, 200, "library", measured::get);
    var catalog = MusicFilters.compact(31, 95, 200, "playlists", measured::get);
    var favorites = MusicFilters.compact(31, 95, 200, "favorites", measured::get);
    assertEquals(library.tabs(), catalog.tabs());
    assertEquals(library.tabs(), favorites.tabs());
    for (var tab : library.tabs()) assertEquals(measured.get(tab.label()) + 12, tab.width());
    var last = library.tabs().get(library.tabs().size() - 1);
    assertTrue(last.x() + last.width() + 6 <= library.addFolderX());
    assertEquals(library.addFolderX(), library.reloadX());
    assertEquals(95, library.actionsY());
    assertEquals(18, library.height());
    assertEquals(231, library.reloadX() + 20);
    assertTrue(173 - (95 + library.height() + 10) >= 20 + 24);
    assertTrue(last.x() + last.width() + 6 + 34 <= 231);
  }

  @Test void shortWideViewsKeepTheNormalGeometry() {
    for (String selector : List.of("library", "playlists", "favorites"))
      assertEquals(MusicFilters.layout(31, 95, 600, selector),
          MusicFilters.compact(31, 95, 600, selector, label -> 40));
  }
}
