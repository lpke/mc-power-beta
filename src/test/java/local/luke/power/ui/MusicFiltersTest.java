package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
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
        assertEquals(133, layout.actionsY());
        assertEquals(40, layout.height());
      }
    }
  }

  @Test void playlistActionsRetainTheirSeparateRow() {
    for (int width : List.of(180, 292, 500))
      assertEquals(18, MusicFilters.layout(31, 111, width, "playlists").height());
  }
}
