package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicFoldersTest {
  @TempDir Path game;

  private Path audio(String path) throws Exception {
    Path file = game.resolve(path);
    Files.createDirectories(file.getParent());
    return Files.write(file, new byte[]{'O','g','g','S',0,0,0,0,0,0,0,0});
  }

  @Test void childImportsAreShallowValidatedAndDeduplicatedAcrossSelectedParents() throws Exception {
    audio("a/one/song.ogg");
    audio("a/two/song.ogg");
    audio("a/deeper/sub/song.ogg");
    audio("b/three/song.ogg");
    Files.createDirectory(game.resolve("a/empty"));
    Files.createDirectory(game.resolve("a/invalid"));
    Files.writeString(game.resolve("a/invalid/not-a-song.mp3"), "not audio at all");
    Files.createSymbolicLink(game.resolve("a/linked"), game.resolve("b/three"));
    var folders = List.of("a/one");
    var added = MusicFolders.additions(game, folders, List.of("a", "a/../a", "b"), true);
    assertEquals(List.of(game.resolve("a/two").toString(), game.resolve("b/three").toString()), added);
    assertEquals(List.of("a/one"), folders);
    assertEquals(List.of(), MusicFolders.additions(game, folders,
        List.of(game.resolve("a/one").toString(), "a/one/../one"), false));
  }

  @Test void invalidBatchesAndFolderLimitsDoNotPartlyImport() throws Exception {
    List<String> existing = new ArrayList<>();
    for (int i = 0; i < 32; i++) {
      String path = "old" + i;
      Files.createDirectory(game.resolve(path));
      existing.add(path);
    }
    audio("new/song.ogg");
    assertThrows(java.io.IOException.class,
        () -> MusicFolders.additions(game, existing, List.of("new"), false));
    assertEquals(32, existing.size());
    assertThrows(java.io.IOException.class,
        () -> MusicFolders.additions(game, List.of(), List.of("new", "missing"), false));
    assertEquals(List.of(game.resolve("new").toString()),
        MusicFolders.additions(game, List.of(), List.of("new", "new"), false));
    assertThrows(java.io.IOException.class,
        () -> MusicFolders.additions(game, List.of(), List.of(" "), false));
  }

  @Test void foldersRoundTripAsExactPathsAndRejectRetiredSourceFlags() {
    var original = JsonParser.parseString("[\"a\",\"b/../b\"]");
    var paths = MusicFolders.decode(original);
    assertEquals(List.of("a", "b/../b"), paths);
    assertEquals(original, MusicFolders.encode(paths));
    for (String bad : List.of("{}", "[null]", "[false]", "[\"a\",\"a\"]",
        "[{\"path\":\"a\",\"enabled\":false}]"))
      assertThrows(IllegalArgumentException.class, () -> MusicFolders.decode(JsonParser.parseString(bad)));
  }

  @Test void sourceRemovalNeverChangesSavedMembership() {
    AudioSettings settings = new AudioSettings();
    settings.musicDirectories.addAll(List.of("first", "second"));
    settings.playlists.add(new MusicPlaylist("saved", "Saved", List.of("music:custom/unavailable"), Map.of()));
    settings.musicDirectories.remove("first");
    assertEquals(List.of("music:custom/unavailable"), settings.playlists.get(0).tracks());
    assertEquals(List.of("second"), settings.musicDirectories);
  }
}
