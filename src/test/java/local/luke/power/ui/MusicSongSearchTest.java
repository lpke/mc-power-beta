package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicSongSearchTest {
  @Test void searchMatchesOnlyLiteralNameSubstringsWithoutChangingOrder() {
    var songs = List.of("Minecraft", "Living Mice", "Mice on Venus", "Sweden");
    assertEquals(List.of("Living Mice", "Mice on Venus"), songs.stream()
        .filter(alias -> MusicLibraryScreen.matchesAlias("  MICE  ", alias)).toList());
    assertFalse(MusicLibraryScreen.matchesAlias("mnecraft", "Minecraft"));
    assertFalse(MusicLibraryScreen.matchesAlias("/music/album", "Minecraft"));
    assertTrue(MusicLibraryScreen.matchesAlias("", "Minecraft"));
  }
}
