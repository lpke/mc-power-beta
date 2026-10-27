package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MusicSongSearchTest {
  @Test void searchRanksNamesBeforeFuzzyMatchesAndKeepsPathMetadataOut() {
    var songs = List.of("Minecraft", "Living Mice", "Mice on Venus", "Sweden");
    assertEquals(List.of("Living Mice", "Mice on Venus"), songs.stream()
        .filter(alias -> MusicLibraryScreen.matchesAlias("  MICE  ", alias)).toList());
    assertTrue(MusicLibraryScreen.matchesAlias("mnecraft", "Minecraft"));
    assertFalse(MusicLibraryScreen.matchesAlias("/music/album", "Minecraft"));
    assertTrue(MusicLibraryScreen.matchesAlias("", "Minecraft"));
    assertEquals(List.of("Mice on Venus", "Living Mice"),
        MusicSearch.filter(songs, "MICE", name -> name));
    assertEquals(List.of("Minecraft"), MusicSearch.filter(songs, "mnecraft", name -> name));
  }
  @Test void exactBuiltinAliasAndFilenameStemRankAheadOfLongerNames() {
    var labels = List.of("moog_city_2.ogg", "moog_city.ogg", "Moog City mix.ogg");
    assertEquals("moog_city.ogg", MusicSearch.tracks(labels, "Moog City", name -> name).get(0));
    assertEquals(0, MusicSearch.trackScore("hal2", "hal2.ogg (Living Mice)"));
    assertEquals(0, MusicSearch.trackScore("Living Mice", "hal2.ogg (Living Mice)"));
    assertEquals(0, MusicSearch.trackScore("Moog City", "moog_city.ogg"));
    assertEquals(0, MusicSearch.trackScore("moog_city", "moog_city.ogg"));
    assertEquals(0, MusicSearch.trackScore("Song", "Song.mp3"));
  }
}
