package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicFoldersTest {
  @TempDir Path game;

  @Test void folderStatesRoundTripAndLegacyPathsStayEnabled() {
    var original = JsonParser.parseString("[\"a\",\"b\",\"a\"]");
    var legacy = MusicFolders.decode(original);
    assertEquals(List.of("a","b"),legacy.paths());
    assertTrue(legacy.disabled().isEmpty());
    var encoded = MusicFolders.encode(legacy.paths(),Set.of("b"));
    var decoded = MusicFolders.decode(encoded);
    assertEquals(legacy.paths(),decoded.paths());
    assertEquals(Set.of("b"),decoded.disabled());
    assertEquals("[\"a\",\"b\",\"a\"]",original.toString());
  }

  @Test void invalidFolderStatesNeverCoerceOrLosePaths() {
    for (String bad : List.of("{}", "[null]", "[false]", "[{\"path\":\"a\"}]",
        "[{\"path\":\"a\",\"enabled\":\"false\"}]"))
      assertThrows(IllegalArgumentException.class, () -> MusicFolders.decode(JsonParser.parseString(bad)));
    var settings = new AudioSettings();
    settings.disabledMusicDirectories.add("a");
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void olderAudioConfigAcquiresEnabledFolderDefaults() {
    var settings = new Gson().fromJson("{\"musicDirectories\":[\"a\"]}",AudioSettings.class);
    assertDoesNotThrow(settings::validate);
    assertTrue(settings.disabledMusicDirectories.isEmpty());
  }

  @Test void overlappingRootsAndContextsRespectTheirOwnSwitches() throws Exception {
    var root = Files.createDirectory(game.resolve("a"));
    var sub = Files.createDirectory(root.resolve("sub"));
    byte[] header = {'O','g','g','S',0,0,0,0,0,0,0,0};
    Files.write(root.resolve("one.ogg"),header);
    Files.write(sub.resolve("two.ogg"),header);
    var scan = MusicLibrary.scan(game,List.of("a","a/sub"),true);
    assertEquals(2,scan.tracks().size());
    assertEquals(0,MusicFolders.enabledTracks(game,scan,List.of("a"),Set.of("a"),true).size());
    assertEquals(1,MusicFolders.enabledTracks(game,scan,List.of("a","a/sub"),Set.of("a"),true).size());
    assertEquals(2,MusicFolders.enabledTracks(game,scan,List.of("a","a/sub"),Set.of("a/sub"),true).size());
    assertEquals(1,MusicFolders.enabledTracks(game,scan,List.of("a"),Set.of(),false).size());
    assertArrayEquals(header,Files.readAllBytes(root.resolve("one.ogg")));
    assertArrayEquals(header,Files.readAllBytes(sub.resolve("two.ogg")));
  }
}
