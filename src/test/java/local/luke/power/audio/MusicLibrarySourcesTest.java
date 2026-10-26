package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicLibrarySourcesTest {
  @TempDir Path game;

  private Path audio(String path) throws Exception {
    Path file = game.resolve(path);
    Files.createDirectories(file.getParent());
    return Files.write(file, new byte[] {'O','g','g','S',0,0,0,0,0,0,0,0});
  }

  private MusicLibrarySources.Snapshot sources(List<String> paths,
      MusicLibrary.Scan scan, boolean recursive) {
    return MusicLibrarySources.build(game, paths, scan, recursive);
  }

  @Test void allConfiguredIdentitiesSurviveEmptyUnavailableAndPendingSources() throws Exception {
    Files.createDirectory(game.resolve("empty"));
    var scan = MusicLibrary.scan(game, List.of("empty", "missing"), true);
    var paths = List.of("empty", "missing", "new/../new");
    var model = sources(paths, scan, true);
    assertEquals(paths, model.folders().stream().map(MusicLibrarySources.Folder::path).toList());
    assertEquals(List.of("Empty", "Unavailable", "Scanning"),
        model.folders().stream().map(MusicLibrarySources.Folder::status).toList());
    assertEquals("source:new/../new", model.folders().get(2).id());
    assertEquals("new", model.folders().get(2).label());
    assertTrue(model.tracks().isEmpty());
    assertFalse(Files.exists(game.resolve("missing")));
  }

  @Test void recursiveTracksBelongToConfiguredRootWithoutChangingTheirPhysicalGainGroup() throws Exception {
    Path file = audio("music/album/deep/song.ogg");
    var scan = MusicLibrary.scan(game, List.of("music"), true);
    var model = sources(List.of("music"), scan, true);
    var track = scan.tracks().get(0);
    assertEquals("Ready", model.folders().get(0).status());
    assertEquals(List.of(track.id()), model.folders().get(0).trackIds());
    assertEquals("source:music", model.trackSources().get(track.id()));
    assertEquals("folder:" + file.getParent(), MusicGroups.id(track.id(), track));
    assertEquals(1, model.tracks().size());
    assertTrue(sources(List.of("music"), scan, false).tracks().isEmpty());
  }

  @Test void mostSpecificImportedRootOwnsEachTrackExactlyOnceRegardlessOfConfiguredOrder() throws Exception {
    audio("music/root.ogg");
    audio("music/album/song.ogg");
    audio("music/album/deep/song.ogg");
    var scan = MusicLibrary.scan(game, List.of("music", "music/album"), true);
    for (var paths : List.of(List.of("music", "music/album"), List.of("music/album", "music"))) {
      var model = sources(paths, scan, true);
      assertEquals(3, model.tracks().size());
      assertEquals(3, model.trackSources().size());
      for (var track : model.tracks()) {
        String expected = track.path().getFileName().toString().equals("root.ogg")
            ? "source:music" : "source:music/album";
        assertEquals(expected, model.trackSources().get(track.id()));
      }
      assertEquals(3, model.folders().stream().mapToInt(folder -> folder.trackIds().size()).sum());
    }
  }

  @Test void aliasesKeepExactConfiguredIdentityAndFirstCanonicalRootWinsTies() throws Exception {
    audio("music/song.ogg");
    // The ancestor symlink is canonicalized on the scanner, never in the view model.
    Files.createSymbolicLink(game.resolve("alias"), game.resolve("music"));
    Files.createDirectories(game.resolve("music/child"));
    audio("music/child/second.ogg");
    var paths = List.of("alias/child", "music/child", "music/child/../child");
    var scan = MusicLibrary.scan(game, paths, true);
    var model = sources(paths, scan, true);
    assertEquals(1, model.tracks().size());
    assertEquals("source:alias/child", model.trackSources().values().iterator().next());
    assertEquals(paths, model.folders().stream().map(MusicLibrarySources.Folder::path).toList());
    assertEquals(List.of("Ready", "Shared", "Shared"),
        model.folders().stream().map(MusicLibrarySources.Folder::status).toList());
  }

  @Test void removalFiltersStaleScanWithoutChangingTrackIdentities() throws Exception {
    audio("a/root.ogg"); audio("a/sub/child.ogg");
    var scan = MusicLibrary.scan(game, List.of("a", "a/sub"), true);
    var child = sources(List.of("a/sub"), scan, true);
    assertEquals(1, child.tracks().size());
    assertEquals("source:a/sub", child.trackSources().values().iterator().next());
    assertEquals(scan.tracks().get(1).id(), child.tracks().get(0).id());
    assertTrue(sources(List.of(), scan, true).tracks().isEmpty());
    assertEquals(2, scan.tracks().size());
  }

  @Test void libraryOrderOnlyChangesGroupPresentationAndKeepsNewGroupsReachable() throws Exception {
    audio("music/root.ogg"); audio("music/sub/child.ogg");
    AudioSettings settings = new AudioSettings();
    settings.musicDirectories.addAll(List.of("music", "music/sub"));
    settings.libraryOrder.addAll(List.of("Alpha", "source:music/sub", "source:removed"));
    settings.validate();
    List<String> order = MusicLibrarySources.order(settings);
    assertEquals(List.of("Alpha", "source:music/sub", "source:music"), order.subList(0, 3));
    assertTrue(order.containsAll(BuiltinMusic.GROUPS));
    assertFalse(order.contains("source:removed"));
    assertEquals(List.of("music", "music/sub"), settings.musicDirectories);
    var scan = MusicLibrary.scan(game, settings.musicDirectories, true);
    var before = sources(settings.musicDirectories, scan, true);
    settings.libraryOrder = new ArrayList<>(List.of("source:music", "Alpha"));
    var after = sources(settings.musicDirectories, scan, true);
    assertEquals(before.trackSources(), after.trackSources());
    assertEquals(before.tracks(), after.tracks());
    AudioSettings restored = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(settings), AudioSettings.class);
    assertEquals(settings.libraryOrder, restored.libraryOrder);
    settings.libraryOrder.add("Alpha");
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void sourceWarningsStayWithTheirConfiguredRootAndScansRemainImmutable() throws Exception {
    audio("good/song.ogg");
    Files.createDirectory(game.resolve("bad"));
    Files.writeString(game.resolve("bad/broken.ogg"), "not an audio stream");
    var scan = MusicLibrary.scan(game, List.of("good", "bad", "missing"), true);
    var model = sources(List.of("good", "bad", "missing"), scan, true);
    assertTrue(model.folders().get(0).warnings().isEmpty());
    assertEquals(List.of("Skipped invalid audio: broken.ogg"), model.folders().get(1).warnings());
    assertEquals(List.of("Folder unavailable: missing"), model.folders().get(2).warnings());
    assertThrows(UnsupportedOperationException.class, () -> scan.sources().clear());
    assertThrows(UnsupportedOperationException.class, () -> scan.sources().get(1).warnings().clear());
    assertThrows(UnsupportedOperationException.class, () -> model.folders().clear());
    assertThrows(UnsupportedOperationException.class, () -> model.trackSources().clear());
    assertThrows(UnsupportedOperationException.class, () -> model.folders().get(0).trackIds().clear());
  }

  @Test void duplicateTrackIdsDoNotCreateRepeatedRowsAndTwoArgumentScansStillWork() {
    var first = new MusicLibrary.Track(game.resolve("music/song.ogg"), "song.ogg");
    var duplicate = new MusicLibrary.Track(game.resolve("music/duplicate.ogg"), "duplicate.ogg", null, first.id());
    var scan = new MusicLibrary.Scan(List.of(first, duplicate), List.of());
    var model = sources(List.of("music"), scan, true);
    assertEquals(List.of(first), model.tracks());
    assertEquals(List.of(first.id()), model.folders().get(0).trackIds());
    assertEquals("Scanning", model.folders().get(0).status());
  }

  @Test void unavailableScannerRootDoesNotClaimStaleTracks() {
    var track = new MusicLibrary.Track(game.resolve("missing/song.ogg"), "song.ogg");
    var scan = new MusicLibrary.Scan(List.of(track), List.of(), List.of(
        new MusicLibrary.SourceFolder("missing", game.resolve("missing"), false, List.of("Unavailable"))));
    var model = sources(List.of("missing"), scan, true);
    assertTrue(model.tracks().isEmpty());
    assertEquals("Unavailable", model.folders().get(0).status());
  }

  @Test void trackLimitRetainsMetadataForEveryRootAndNeverAddsDuplicateTracks() throws Exception {
    Path root = Files.createDirectory(game.resolve("many"));
    for (int index = 0; index <= MusicLibrary.MAX_TRACKS; index++)
      Files.write(root.resolve("song" + index + ".mus"), new byte[4]);
    Files.createDirectory(root.resolve("child"));
    var paths = List.of("many", "many/child", "missing");
    var scan = MusicLibrary.scan(game, paths, true);
    assertEquals(MusicLibrary.MAX_TRACKS, scan.tracks().size());
    assertEquals(MusicLibrary.MAX_TRACKS, scan.tracks().stream().map(MusicLibrary.Track::id).distinct().count());
    assertEquals(paths, scan.sources().stream().map(MusicLibrary.SourceFolder::path).toList());
    assertTrue(scan.warnings().contains("Music library reached the 2,048 track limit"));
    assertEquals("Incomplete", sources(paths, scan, true).folders().get(0).status());
    assertFalse(scan.sources().get(1).warnings().isEmpty());
    assertEquals("Incomplete", sources(paths, scan, true).folders().get(1).status());
    assertFalse(scan.sources().get(2).available());
    assertEquals(MusicLibrary.MAX_TRACKS, sources(paths, scan, true).tracks().size());
  }

  @Test void partiallyReadFolderKeepsItsTracksAndReportsIncomplete() {
    var track = new MusicLibrary.Track(game.resolve("music/song.ogg"), "song.ogg");
    var scan = new MusicLibrary.Scan(List.of(track), List.of(), List.of(
        new MusicLibrary.SourceFolder("music", game.resolve("music"), true,
            List.of("Could not finish reading: music"))));
    var model = sources(List.of("music"), scan, true);
    assertEquals("Incomplete", model.folders().get(0).status());
    assertEquals(List.of(track.id()), model.folders().get(0).trackIds());
    assertEquals(List.of(track), model.tracks());
  }

  @Test void visitedLimitStopsWalkingButKeepsLaterRootIdentityAndIncompleteStatus() throws Exception {
    Path root = Files.createDirectory(game.resolve("many"));
    for (int index = 0; index < 700; index++) Files.writeString(root.resolve("ignored" + index + ".txt"), "data");
    List<String> paths = new ArrayList<>();
    for (int index = 0; index < MusicFolders.MAX_FOLDERS; index++) paths.add("many" + "/.".repeat(index));
    var scan = MusicLibrary.scan(game, paths, true);
    assertTrue(scan.tracks().isEmpty());
    assertEquals(paths, scan.sources().stream().map(MusicLibrary.SourceFolder::path).toList());
    assertTrue(scan.warnings().contains("Music scan reached the 20,000 file limit"));
    var finalSource = sources(paths, scan, true).folders().get(paths.size() - 1);
    assertEquals(paths.get(paths.size() - 1), finalSource.path());
    assertEquals("Incomplete", finalSource.status());
    assertEquals(List.of("Folder scan incomplete: music scan limit reached"), finalSource.warnings());
  }

}
