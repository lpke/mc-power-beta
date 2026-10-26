package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class MusicPlaylistTest {
  @Test void orderedSelectionsAndMissingIdsRoundTripWithTheGamesGson() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("one", "One", List.of("music:z", "music:missing", "music:a"), Map.of("Alpha", 40)));
    MusicPlaylist.load(settings, "one");
    Gson gson = new Gson();
    settings = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    settings.validate();
    assertEquals(List.of("music:z", "music:missing", "music:a"), settings.playlistTracks);
    assertEquals(settings.playlistTracks, settings.playlists.get(0).tracks());
    assertFalse(settings.playlists.get(0).includes("music:new"));
  }

  @Test void savedWorldAndMenuSnapshotsStayIndependentUntilLoaded() {
    AudioSettings settings = new AudioSettings();
    List<String> tracks = new ArrayList<>(List.of("music:a", "music:b"));
    Map<String, Integer> groups = new TreeMap<>(Map.of("Alpha", 30));
    MusicPlaylist saved = new MusicPlaylist("one", "One", tracks, groups);
    tracks.clear(); groups.clear();
    settings.playlists.add(saved);
    MusicPlaylist.load(settings, "one");
    MusicPlaylist.loadMenu(settings, "one");
    settings.playlistTracks.remove("music:a");
    settings.groupVolumes.put("Alpha", 60);
    assertEquals(List.of("music:a", "music:b"), saved.tracks());
    assertEquals(saved.tracks(), settings.menuPlaylistTracks);
    assertEquals(30, settings.menuGroupVolumes.get("Alpha"));
    settings.playlists.set(0, new MusicPlaylist("one", "Edited", List.of("music:c"), Map.of("Alpha", 10)));
    assertEquals(List.of("music:b"), settings.playlistTracks);
    assertEquals(List.of("music:a", "music:b"), settings.menuPlaylistTracks);
    MusicPlaylist.loadMenu(settings, "one");
    assertEquals(List.of("music:c"), settings.menuPlaylistTracks);
    assertEquals(List.of("music:b"), settings.playlistTracks);
    MusicPlaylist.load(settings, "one");
    assertEquals(List.of("music:c"), settings.playlistTracks);
    assertThrows(UnsupportedOperationException.class, () -> saved.tracks().add("music:c"));
  }

  @Test void emptyPlaylistIsExplicitAndDefaultExclusionsRemainIndependent() {
    AudioSettings settings = new AudioSettings();
    settings.disabledTracks.add("music:a");
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of(), Map.of()));
    MusicPlaylist.load(settings, "empty");
    assertEquals("empty", settings.playlist);
    assertTrue(settings.playlistTracks.isEmpty());
    assertEquals(Set.of("music:a"), settings.disabledTracks);
    MusicPlaylist.load(settings, "");
    assertEquals("", settings.playlist);
    assertEquals(Set.of("music:a"), settings.disabledTracks);
    assertEquals(AudioSettings.MusicMode.VANILLA, settings.musicMode);
  }

  @Test void menuSentinelsDoNotCollideWithSavedIdsOrChangeTransitionPolicy() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("same", "Same", List.of("music:a"), Map.of()));
    MusicPlaylist.loadMenu(settings, "same");
    assertEquals(List.of("music:a"), settings.menuPlaylistTracks);
    for (String selector : List.of("same", MusicPlaylist.SAME_AS_WORLD, MusicPlaylist.DEFAULT)) {
      MusicPlaylist.loadMenu(settings, selector);
      for (var mode : AudioSettings.MenuMusic.values()) {
        settings.menuMusic = mode;
        assertEquals(mode != AudioSettings.MenuMusic.CUSTOM, settings.continuesAcrossMenus());
      }
    }
  }

  @Test void malformedSnapshotsCannotPartlyLoadOrSave() {
    assertThrows(IllegalArgumentException.class, () -> new MusicPlaylist("bad/id", "Bad", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, () -> new MusicPlaylist("id", " ", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, () -> new MusicPlaylist("id", "Name", List.of("music:a", "music:a"), Map.of()));
    AudioSettings settings = new AudioSettings();
    settings.playlistTracks.add("music:a");
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, "missing"));
    assertEquals(List.of("music:a"), settings.playlistTracks);
    MusicPlaylist invalid = new Gson().fromJson("{\"id\":\"bad\",\"name\":\"Bad\",\"tracks\":[\"music:b\"],\"groupVolumes\":{\"Alpha\":101}}", MusicPlaylist.class);
    settings.playlists.add(invalid);
    assertThrows(IllegalArgumentException.class, settings::validate);
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, "bad"));
    assertEquals(List.of("music:a"), settings.playlistTracks);
    assertEquals("", settings.playlist);
  }

  @Test void addingLibraryTracksDoesNotExpandSavedOrWorkingSelections() {
    String old = "music:custom/old", added = "music:custom/new", missing = "music:custom/missing";
    AudioSettings settings = new AudioSettings();
    MusicPlaylist saved = new MusicPlaylist("one", "One", List.of(missing, old), Map.of());
    settings.playlists.add(saved);
    MusicPlaylist.load(settings, "one");
    List<String> rescannedLibrary = List.of(old, added);
    assertEquals(List.of(old), settings.playlistTracks.stream().filter(rescannedLibrary::contains).toList());
    assertEquals(List.of(missing, old), settings.playlistTracks);
    assertFalse(saved.includes(added));
    settings.playlistTracks.add(added);
    assertFalse(saved.includes(added));
    MusicPlaylist.load(settings, "one");
    assertEquals(List.of(missing, old), settings.playlistTracks);
  }

  @Test void playlistAndQueueHaveSeparateOrderingAndDuplicateRules() {
    MusicQueue queue = new MusicQueue();
    queue.add("music:missing"); queue.add("music:a"); queue.add("music:missing");
    MusicPlaylist saved = new MusicPlaylist("one", "One", List.of("music:a", "music:missing"), Map.of());
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(saved);
    MusicPlaylist.load(settings, "one");
    settings.playlistTracks.remove("music:missing");
    Gson gson = new Gson();
    AudioSettings restoredSettings = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    MusicQueue restoredQueue = gson.fromJson(gson.toJson(queue), MusicQueue.class);
    restoredSettings.validate(); restoredQueue.validate();
    assertEquals(List.of("music:missing", "music:a", "music:missing"), restoredQueue.tracks);
    assertEquals(List.of("music:a"), restoredSettings.playlistTracks);
    assertEquals(List.of("music:a", "music:missing"), restoredSettings.playlists.get(0).tracks());
    assertFalse(gson.toJsonTree(restoredSettings).getAsJsonObject().has("musicQueue"));
  }

  @Test void incompleteSelectionsAndDuplicateIdentifiersAreRejected() {
    for (String fields : List.of("", ",\"groupVolumes\":{}", ",\"tracks\":[]")) {
      MusicPlaylist incomplete = new Gson().fromJson("{\"id\":\"one\",\"name\":\"One\"" + fields + "}", MusicPlaylist.class);
      AudioSettings settings = new AudioSettings();
      settings.playlists.add(incomplete);
      assertThrows(IllegalArgumentException.class, settings::validate);
      assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.loadMenu(settings, "one"));
      assertEquals(MusicPlaylist.SAME_AS_WORLD, settings.menuPlaylist);
      assertTrue(settings.menuPlaylistTracks.isEmpty());
    }
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("one", "One", List.of(), Map.of()));
    settings.playlists.add(new MusicPlaylist("one", "Duplicate", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

}
