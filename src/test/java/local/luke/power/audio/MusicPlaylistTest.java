package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class MusicPlaylistTest {
  private AudioSettings selected() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("one", "One", List.of("music:z", "music:missing", "music:a")));
    settings.libraryGroupVolumes.put("Alpha", 40);
    MusicPlaylist.load(settings, "one");
    return settings;
  }

  @Test void betaIsFixedReservedAndDefaultWithoutSavingASecondCopy() {
    AudioSettings settings = new AudioSettings();
    assertEquals("beta", settings.playlist);
    assertEquals(BuiltinMusic.VANILLA, MusicPlaylist.world(settings).tracks());
    assertEquals(12, MusicPlaylist.beta().tracks().size());
    assertEquals(List.of("beta", "favorites"), MusicPlaylist.all(settings).stream().map(MusicPlaylist::id).toList());
    assertTrue(settings.playlists.isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> MusicPlaylist.beta().tracks().clear());
    for (String id : List.of("beta", "favorites")) {
      settings.playlists = new ArrayList<>(List.of(new MusicPlaylist(id, "Fake", List.of())));
      assertThrows(IllegalArgumentException.class, settings::validate);
    }
  }

  @Test void orderedSelectionsMissingIdsAndLibraryGainsRoundTripWithTheGamesGson() {
    AudioSettings settings = selected();
    settings.playlistOrder.addAll(List.of("one", "favorites", "beta"));
    Gson gson = new Gson();
    AudioSettings restored = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    restored.validate();
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.world(restored).tracks());
    assertEquals(Map.of("Alpha", 40), restored.libraryGroupVolumes);
    assertEquals(List.of("one", "favorites", "beta"), MusicPlaylist.all(restored).stream().map(MusicPlaylist::id).toList());
    assertSame(restored.playlists.get(0), MusicPlaylist.world(restored));
    var playlistJson = gson.toJsonTree(MusicPlaylist.world(restored)).getAsJsonObject();
    assertEquals(Set.of("id", "name", "tracks"), playlistJson.keySet());
    assertFalse(gson.toJsonTree(restored).getAsJsonObject().has("favoritesGroupVolumes"));
  }

  @Test void allSelectionsUseGlobalLibraryGainsWithoutEditingMembership() {
    AudioSettings settings = selected();
    settings.favourites.addAll(List.of("music:a", "music:favorite"));
    settings.playlists.add(new MusicPlaylist("menu", "Menu", List.of("music:a", "music:menu")));
    MusicPlaylist.loadMenu(settings, "menu");
    assertEquals(List.of("music:a", "music:menu"), MusicPlaylist.rotation(settings, true));
    for (String id : List.of("beta", "one", "favorites", "menu")) {
      List<String> before = MusicPlaylist.find(settings, id).tracks();
      MusicPlaylist.load(settings, id);
      assertEquals(.4f, MusicGroups.gain(settings.libraryGroupVolumes, "Alpha"), .00001f);
      settings.libraryGroupVolumes.put("Alpha", 30);
      assertEquals(before, MusicPlaylist.world(settings).tracks());
      assertEquals(.3f, MusicGroups.gain(settings.libraryGroupVolumes, "Alpha"), .00001f);
      settings.libraryGroupVolumes.put("Alpha", 40);
    }
  }

  @Test void favoritesMembershipStaysLiveWhileSnapshotsStayImmutable() {
    AudioSettings settings = selected();
    settings.favourites.addAll(List.of("music:a", "music:missing"));
    MusicPlaylist.load(settings, "favorites");
    MusicPlaylist snapshot = MusicPlaylist.world(settings);
    settings.favourites.remove("music:a"); settings.favourites.add("music:z");
    assertEquals(List.of("music:missing", "music:z"), MusicPlaylist.rotation(settings, false));
    assertEquals(List.of("music:a", "music:missing"), snapshot.tracks());
    assertTrue(MusicPlaylist.contains(settings, "@world", "music:z"));
    assertFalse(MusicPlaylist.contains(settings, "@world", "music:a"));
  }

  @Test void catalogOrderIncludesProtectedPlaylistsAndKeepsUnspecifiedNewEntries() {
    AudioSettings settings = selected();
    settings.playlistOrder.addAll(List.of("favorites", "one", "beta"));
    settings.playlists.add(new MusicPlaylist("new", "New", List.of()));
    settings.validate();
    assertEquals(List.of("favorites", "one", "beta", "new"), MusicPlaylist.all(settings).stream().map(MusicPlaylist::id).toList());
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.find(settings, "one").tracks());
    settings.playlistOrder.add("one");
    assertThrows(IllegalArgumentException.class, settings::validate);
    settings.playlistOrder = new ArrayList<>(List.of("gone"));
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void menuAliasSharesWorldSelectionAndSeparateMenuUsesItsOwnMembership() {
    AudioSettings settings = selected();
    assertEquals(MusicPlaylist.selectionId(settings, false), MusicPlaylist.selectionId(settings, true));
    MusicPlaylist.loadMenu(settings, "one");
    assertSame(MusicPlaylist.world(settings), MusicPlaylist.menu(settings));
    MusicPlaylist.load(settings, "beta");
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.rotation(settings, true));
    settings.playlists.set(0, new MusicPlaylist("one", "Edited", List.of("music:c")));
    assertEquals(List.of("music:c"), MusicPlaylist.rotation(settings, true));
    assertEquals(Map.of("Alpha", 40), settings.libraryGroupVolumes);
  }

  @Test void invalidSavedSelectionsAndMalformedMembershipCannotPartlyLoad() {
    AudioSettings settings = selected();
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, "missing"));
    assertEquals("one", settings.playlist);
    for (String fields : List.of("", ",\"tracks\":null", ",\"tracks\":[\"music:a\",\"music:a\"]")) {
      var malformed = new Gson().fromJson("{\"id\":\"bad\",\"name\":\"Bad\"" + fields + "}", MusicPlaylist.class);
      settings.playlists.add(malformed);
      assertThrows(IllegalArgumentException.class, settings::validate);
      assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.loadMenu(settings, "bad"));
      settings.playlists.remove(malformed);
    }
    assertEquals("@world", settings.menuPlaylist);
    settings.libraryGroupVolumes.put("Alpha", 101);
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void discoveryRetainsMissingPlaylistFavoriteAndGainReferencesWithoutExpandingMembership() {
    AudioSettings settings = selected();
    settings.favourites.add("music:favorite");
    settings.sounds.put("music:retained", 42);
    settings.sounds.put("random.click", 55);
    List<String> before = MusicPlaylist.world(settings).tracks();
    settings.musicDirectories.add("new-folder");
    List<String> ids = settings.musicTrackIds(List.of("music:available", "music:z"));
    assertTrue(ids.containsAll(List.of("music:missing", "music:favorite", "music:retained")));
    assertTrue(ids.containsAll(BuiltinMusic.VANILLA));
    assertEquals(ids.size(), new HashSet<>(ids).size());
    assertFalse(ids.contains("random.click"));
    assertEquals(before, MusicPlaylist.world(settings).tracks());
    assertThrows(UnsupportedOperationException.class, () -> ids.add("music:mutated"));
  }
}
