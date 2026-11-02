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

  @Test void multipleMissingRootsKeepSeparateCachesAndRelinkIndependentlyWithoutChangingMembership() throws Exception {
    var paths=List.of("old/alpha/music","old/beta/music","old/missing");
    Path alpha=song(paths.get(0),"track.mp3"),beta=song(paths.get(1),"track.mp3");
    Path alphaCache=cache(alpha),betaCache=cache(beta);
    var original=MusicLibrary.scan(game,paths);
    String alphaId=original.tracks().stream().filter(track->track.path().equals(alpha)).findFirst().orElseThrow().id();
    String betaId=original.tracks().stream().filter(track->track.path().equals(beta)).findFirst().orElseThrow().id();
    assertNotEquals(alphaId,betaId);assertNotEquals(alphaCache,betaCache);
    var settings=new AudioSettings();settings.musicDirectories.addAll(paths);
    settings.playlists.add(new MusicPlaylist("saved","Saved",List.of(betaId,alphaId)));
    settings.playlist="saved";settings.favourites.addAll(List.of(alphaId,betaId));
    settings.sounds.put(alphaId,65);settings.sounds.put(betaId,35);
    settings.libraryOrder.addAll(paths.stream().map(MusicLibrarySources::id).toList());
    settings.libraryGroupVolumes.put(MusicLibrarySources.id(paths.get(0)),72);
    settings.libraryGroupVolumes.put(MusicLibrarySources.id(paths.get(1)),44);
    Files.createDirectories(game.resolve("moved/alpha"));Files.createDirectories(game.resolve("moved/beta"));
    Files.move(alpha.getParent(),game.resolve("moved/alpha/music"));Files.move(beta.getParent(),game.resolve("moved/beta/music"));
    var missing=MusicLibrary.scan(game,paths);
    var model=MusicLibrarySources.build(game,paths,missing);
    assertEquals(paths.stream().map(path->"Folder unavailable: "+path).toList(),missing.warnings());
    assertEquals(List.of("Unavailable","Unavailable","Unavailable"),model.folders().stream().map(MusicLibrarySources.Folder::status).toList());
    assertEquals(List.of(alphaId),model.folders().get(0).trackIds());
    assertEquals(List.of(betaId),model.folders().get(1).trackIds());assertTrue(model.folders().get(2).trackIds().isEmpty());
    assertEquals(Set.of(alphaCache,betaCache),new HashSet<>(model.tracks().stream().map(MusicLibrary.Track::playbackPath).toList()));
    var first=MusicFolders.relink(game,settings,paths.get(0),"moved/alpha/music");
    var partly=MusicLibrarySources.build(game,first.musicDirectories,MusicLibrary.scan(game,first.musicDirectories,first.musicSourceIdentities));
    assertEquals(List.of("Ready","Unavailable","Unavailable"),partly.folders().stream().map(MusicLibrarySources.Folder::status).toList());
    assertEquals(List.of(betaId),partly.folders().get(1).trackIds());
    var second=MusicFolders.relink(game,first,paths.get(1),"moved/beta/music");
    var gson=new com.google.gson.Gson();second=gson.fromJson(gson.toJson(second),AudioSettings.class);second.validate();
    var restored=MusicLibrary.scan(game,second.musicDirectories,second.musicSourceIdentities);
    assertEquals(Set.of(alphaId,betaId),new HashSet<>(restored.tracks().stream().map(MusicLibrary.Track::id).toList()));
    assertEquals(Set.of(alphaCache,betaCache),new HashSet<>(restored.tracks().stream().map(MusicLibrary.Track::playbackPath).toList()));
    assertEquals(gson.toJsonTree(settings.playlists),gson.toJsonTree(second.playlists));assertEquals(settings.playlist,second.playlist);
    assertEquals(settings.favourites,second.favourites);assertEquals(settings.sounds,second.sounds);
    assertEquals(game.resolve(paths.get(0)).toString(),second.musicSourceIdentities.get(game.resolve("moved/alpha/music").toString()));
    assertEquals(game.resolve(paths.get(1)).toString(),second.musicSourceIdentities.get(game.resolve("moved/beta/music").toString()));
    assertEquals(72,second.libraryGroupVolumes.get(MusicLibrarySources.id(game.resolve("moved/alpha/music").toString())));
    assertEquals(44,second.libraryGroupVolumes.get(MusicLibrarySources.id(game.resolve("moved/beta/music").toString())));
    assertEquals(List.of(MusicLibrarySources.id(game.resolve("moved/alpha/music").toString()),
        MusicLibrarySources.id(game.resolve("moved/beta/music").toString()),MusicLibrarySources.id(paths.get(2))),second.libraryOrder);
    assertEquals(paths,settings.musicDirectories);assertTrue(settings.musicSourceIdentities.isEmpty());
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
