package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicRelinkTest {
  @TempDir Path game;

  private Path song(String folder, String name) throws Exception {
    Path file = game.resolve(folder).resolve(name);
    Files.createDirectories(file.getParent());
    return Files.write(file, new byte[]{'I','D','3',0,0,0,0,0,0,0,0,0});
  }

  private Path cache(Path source) throws Exception {
    Path target = game.resolve("power-beta-data/music-cache").resolve(Mp3Converter.hash(
        source.toAbsolutePath().normalize() + ":" + Files.size(source) + ":"
            + Files.getLastModifiedTime(source).toMillis()) + ".wav");
    Files.createDirectories(target.getParent());
    return Files.write(target, new byte[100]);
  }

  @Test void repeatedRelinksKeepPlaylistTrackIdsFavoritesGainsAndCachedPlayback() throws Exception {
    Path original = song("old/album", "track.mp3");
    Path cached = cache(original);
    var first = MusicLibrary.scan(game, List.of("old"));
    String id = first.tracks().get(0).id();
    AudioSettings settings = new AudioSettings();
    settings.musicDirectories.add("old");
    settings.playlists.add(new MusicPlaylist("test", "Test", List.of(id)));
    settings.favourites.add(id); settings.sounds.put(id, 65);
    settings.libraryGroupVolumes.put("source:old", 72); settings.libraryOrder.add("source:old");
    Files.move(game.resolve("old"), game.resolve("new"));
    AudioSettings moved = MusicFolders.relink(game, settings, "old", "new");
    var scan = MusicLibrary.scan(game, moved.musicDirectories, moved.musicSourceIdentities);
    assertEquals(id, scan.tracks().get(0).id());
    assertEquals(cached, scan.tracks().get(0).playbackPath());
    assertEquals(settings.playlists.get(0).id(), moved.playlists.get(0).id());
    assertEquals(settings.playlists.get(0).name(), moved.playlists.get(0).name());
    assertEquals(settings.playlists.get(0).tracks(), moved.playlists.get(0).tracks());
    assertEquals(settings.favourites, moved.favourites); assertEquals(settings.sounds, moved.sounds);
    assertEquals(72, moved.libraryGroupVolumes.get("source:" + game.resolve("new")));
    assertEquals(List.of("old"), settings.musicDirectories);
    Files.move(game.resolve("new"), game.resolve("again"));
    moved = MusicFolders.relink(game, moved, game.resolve("new").toString(), "again");
    assertEquals(id, MusicLibrary.scan(game, moved.musicDirectories, moved.musicSourceIdentities).tracks().get(0).id());
  }

  @Test void missingFolderRetainsConvertedSongAcrossScansAndReportsUnavailable() throws Exception {
    Path source = song("old", "track.mp3"), cached = cache(source);
    var original = MusicLibrary.scan(game, List.of("old"));
    Files.move(game.resolve("old"), game.resolve("away"));
    var missing = MusicLibrary.scan(game, List.of("old"));
    assertEquals(original.tracks(), missing.tracks());
    assertEquals(cached, missing.tracks().get(0).playbackPath());
    var groups = MusicLibrarySources.build(game, List.of("old"), missing);
    assertEquals("Unavailable", groups.folders().get(0).status());
    assertEquals(1, groups.tracks().size());
    Files.delete(cached);
    assertTrue(MusicLibrary.scan(game, List.of("old")).tracks().isEmpty());
  }

  @Test void changedSourceDoesNotReuseOldConversionAndUnimportingHidesCache() throws Exception {
    Path source = song("music", "track.mp3"); cache(source);
    MusicLibrary.scan(game, List.of("music"));
    Files.setLastModifiedTime(source, FileTime.fromMillis(Files.getLastModifiedTime(source).toMillis() + 10_000));
    assertFalse(MusicLibrary.scan(game, List.of("music")).tracks().get(0).playable());
    assertTrue(MusicLibrary.scan(game, List.of()).tracks().isEmpty());
  }

  @Test void unavailableOrDuplicateRelinkDoesNotMutateSettings() throws Exception {
    Files.createDirectories(game.resolve("other"));
    AudioSettings settings = new AudioSettings(); settings.musicDirectories.addAll(List.of("old", "other"));
    assertThrows(java.io.IOException.class, () -> MusicFolders.relink(game, settings, "old", "missing"));
    assertThrows(java.io.IOException.class, () -> MusicFolders.relink(game, settings, "old", "other"));
    assertEquals(List.of("old", "other"), settings.musicDirectories);
    assertTrue(settings.musicSourceIdentities.isEmpty());
  }

  @Test void relinkSurvivesGsonRestartAndOverlappingImports() throws Exception {
    Path original = song("old/sub", "track.mp3");
    String id = MusicLibrary.scan(game, List.of("old")).tracks().get(0).id();
    AudioSettings settings = new AudioSettings(); settings.musicDirectories.add("old");
    Files.move(game.resolve("old"), game.resolve("new"));
    settings = MusicFolders.relink(game, settings, "old", "new");
    settings.musicDirectories.add(0, game.resolve("new/sub").toString());
    var json = new com.google.gson.Gson();
    settings = json.fromJson(json.toJson(settings), AudioSettings.class);
    settings.validate();
    assertEquals(id, MusicLibrary.scan(game, settings.musicDirectories, settings.musicSourceIdentities).tracks().get(0).id());
  }

  @Test void invalidCacheIndexIsPreservedAndDoesNotAbortValidSourceScanning() throws Exception {
    song("music", "first.mp3"); song("music", "second.mp3");
    Path index = game.resolve("power-beta-data/music-cache/index.json"); Files.createDirectories(index.getParent());
    String damaged = "{\"version\":1,\"tracks\":{\"bad\":{}}}"; Files.writeString(index, damaged);
    var scan = MusicLibrary.scan(game, List.of("music"));
    assertEquals(2, scan.tracks().size());
    assertFalse(scan.warnings().isEmpty()); assertEquals(damaged, Files.readString(index));
    var unimported = MusicLibrary.scan(game, List.of());
    assertTrue(unimported.tracks().isEmpty()); assertTrue(unimported.warnings().isEmpty());
    assertEquals(damaged, Files.readString(index));
  }

  @Test void relinkingNestedImportRetainsTheIdentityInheritedFromItsRelinkedParent() throws Exception {
    song("old/sub", "track.mp3");
    String id = MusicLibrary.scan(game, List.of("old")).tracks().get(0).id();
    AudioSettings settings = new AudioSettings(); settings.musicDirectories.add("old");
    Files.move(game.resolve("old"), game.resolve("new"));
    settings = MusicFolders.relink(game, settings, "old", "new");
    String nested = game.resolve("new/sub").toString(); settings.musicDirectories.add(nested);
    assertEquals(id, MusicLibrary.scan(game, settings.musicDirectories, settings.musicSourceIdentities).tracks().get(0).id());
    Files.move(game.resolve("new/sub"), game.resolve("replacement"));
    settings = MusicFolders.relink(game, settings, nested, "replacement");
    assertEquals(game.resolve("old/sub").toString(), settings.musicSourceIdentities.get(game.resolve("replacement").toString()));
    assertEquals(id, MusicLibrary.scan(game, settings.musicDirectories, settings.musicSourceIdentities).tracks().get(0).id());
  }

  @Test void missingNestedImportRetainsCacheThroughItsRelinkedParentIdentity() throws Exception {
    Path original = song("old/sub", "track.mp3"), cached = cache(original);
    String id = MusicLibrary.scan(game, List.of("old")).tracks().get(0).id();
    AudioSettings settings = new AudioSettings(); settings.musicDirectories.add("old");
    Files.move(game.resolve("old"), game.resolve("new"));
    settings = MusicFolders.relink(game, settings, "old", "new");
    settings.musicDirectories.add(game.resolve("new/sub").toString());
    Files.move(game.resolve("new/sub"), game.resolve("away"));
    var missing = MusicLibrary.scan(game, settings.musicDirectories, settings.musicSourceIdentities);
    assertEquals(1, missing.tracks().size());
    assertEquals(id, missing.tracks().get(0).id());
    assertEquals(cached, missing.tracks().get(0).playbackPath());
  }
}
