package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicTabNavigationTest {
  @Test void switchingTabsRetainsEditorsWithoutCreatingLocalBackRoutes() {
    var navigation = new MusicTabNavigation<String>();
    navigation.remember("playlists", "catalog query/scroll");
    navigation.leave("playlists", "editor query/scroll/grouping/add-browser");
    navigation.leave("library", "library query/scroll");
    assertEquals("editor query/scroll/grouping/add-browser", navigation.view("playlists"));
    assertEquals("catalog query/scroll", navigation.history("playlists").peek());
    assertTrue(navigation.history("library").isEmpty());
    assertTrue(navigation.history("active").isEmpty());
    assertTrue(navigation.history("favorites").isEmpty());
  }

  @Test void repeatedPlaylistsClickResetsItsCatalogWithoutChangingOtherTabs() {
    var navigation = new MusicTabNavigation<String>();
    navigation.remember("playlists", "catalog");
    navigation.leave("playlists", "editor");
    navigation.leave("library", "search");
    navigation.reset("playlists");
    assertNull(navigation.view("playlists"));
    assertTrue(navigation.history("playlists").isEmpty());
    assertEquals("search", navigation.view("library"));
  }

  @Test void leavingMusicClearsEveryTabAndLocalHistoryIsBounded() {
    var navigation = new MusicTabNavigation<String>();
    for (int i = 0; i < 40; i++) navigation.remember("playlists", "view " + i);
    assertEquals(32, navigation.history("playlists").size());
    assertEquals("view 39", navigation.history("playlists").peek());
    assertEquals("view 8", navigation.history("playlists").peekLast());
    navigation.leave("playlists", "editor");
    navigation.clear();
    assertNull(navigation.view("playlists"));
    assertTrue(navigation.history("playlists").isEmpty());
  }
}
