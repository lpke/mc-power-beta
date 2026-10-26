package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class MusicPlaylistTest {
  private AudioSettings selected() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("one", "One", List.of("music:z", "music:missing", "music:a"), Map.of("Alpha", 40)));
    MusicPlaylist.load(settings, "one");
    return settings;
  }

  @Test void betaIsFixedReservedAndDefaultWithoutSavingASecondCopy() {
    AudioSettings settings = new AudioSettings();
    assertEquals("beta", settings.playlist);
    assertEquals(12, MusicPlaylist.beta().tracks().size());
    assertEquals(BuiltinMusic.VANILLA, MusicPlaylist.world(settings).tracks());
    assertEquals(List.of(MusicPlaylist.BETA, MusicPlaylist.FAVORITES),
        MusicPlaylist.all(settings).stream().map(MusicPlaylist::id).toList());
    assertTrue(settings.playlists.isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> MusicPlaylist.beta().tracks().clear());
    settings.playlists.add(new MusicPlaylist("beta", "Fake Beta", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void orderedSelectionsAndMissingIdsRoundTripWithTheGamesGson() {
    AudioSettings settings = selected();
    Gson gson = new Gson();
    settings = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    settings.validate();
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.world(settings).tracks());
    assertEquals(40, MusicPlaylist.world(settings).groupVolumes().get("Alpha"));
    assertSame(settings.playlists.get(0), MusicPlaylist.world(settings));
    assertFalse(gson.toJsonTree(settings).getAsJsonObject().has("playlistTracks"));
  }

  @Test void editingSelectedSavedObjectImmediatelyChangesWorldAndMenuTruth() {
    AudioSettings settings = selected();
    MusicPlaylist.loadMenu(settings, "one");
    settings.playlists.set(0, new MusicPlaylist("one", "Edited", List.of("music:c"), Map.of("Alpha", 10)));
    assertSame(settings.playlists.get(0), MusicPlaylist.world(settings));
    assertSame(MusicPlaylist.world(settings), MusicPlaylist.menu(settings));
    assertEquals(List.of("music:c"), MusicPlaylist.rotation(settings, false));
    assertEquals(10, MusicPlaylist.menu(settings).groupVolumes().get("Alpha"));
    MusicPlaylist.load(settings, MusicPlaylist.BETA);
    assertEquals("one", settings.menuPlaylist);
    assertEquals(List.of("music:c"), MusicPlaylist.menu(settings).tracks());
  }

  @Test void menuSelectionUsesOnlyItsSavedOrderAndItsOwnGains() {
    AudioSettings settings = selected();
    settings.playlists.add(new MusicPlaylist("menu", "Menu", List.of("music:a", "music:menu"), Map.of("Alpha", 20)));
    MusicPlaylist.loadMenu(settings, "menu");
    assertEquals(List.of("music:a", "music:menu"), MusicPlaylist.rotation(settings, true));
    assertEquals(MusicPlaylist.world(settings).tracks(), MusicPlaylist.rotation(settings, false));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:menu", true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", false));
    MusicPlaylist.loadMenu(settings, "one");
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD);
    assertEquals(MusicPlaylist.world(settings).tracks(), MusicPlaylist.rotation(settings, true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
  }

  @Test void worldAliasAndExplicitSamePlaylistShareThePlaybackContext() {
    AudioSettings settings = selected();
    assertEquals(MusicPlaylist.selectionId(settings, false), MusicPlaylist.selectionId(settings, true));
    MusicPlaylist.loadMenu(settings, "one");
    assertEquals(MusicPlaylist.selectionId(settings, false), MusicPlaylist.selectionId(settings, true));
    MusicPlaylist.loadMenu(settings, MusicPlaylist.BETA);
    assertNotEquals(MusicPlaylist.selectionId(settings, false), MusicPlaylist.selectionId(settings, true));
  }

  @Test void emptyPlaylistStaysSilentAndFolderDiscoveriesCannotExpandMembership() {
    AudioSettings settings = selected();
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of(), Map.of()));
    MusicPlaylist.load(settings, "empty");
    assertTrue(MusicPlaylist.rotation(settings, false).isEmpty());
    settings.musicDirectories.add("new-folder");
    assertTrue(MusicPlaylist.world(settings).tracks().isEmpty());
    MusicPlaylist.load(settings, "one");
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.world(settings).tracks());
  }

  @Test void malformedAndRemovedSelectionsCannotPartlyLoadOrSave() {
    AudioSettings settings = selected();
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, "missing"));
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, ""));
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.loadMenu(settings, "@default"));
    assertEquals("one", settings.playlist);
    assertEquals("@world", settings.menuPlaylist);
    MusicPlaylist invalid = new Gson().fromJson("{\"id\":\"bad\",\"name\":\"Bad\",\"tracks\":[\"music:b\"],\"groupVolumes\":{\"Alpha\":101}}", MusicPlaylist.class);
    settings.playlists.add(invalid);
    assertThrows(IllegalArgumentException.class, settings::validate);
    assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.load(settings, "bad"));
    assertEquals("one", settings.playlist);
    settings.playlists.clear();
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void playlistAndQueueHaveSeparateOrderingAndDuplicateRules() {
    MusicQueue queue = new MusicQueue();
    queue.add("music:missing"); queue.add("music:a"); queue.add("music:missing");
    AudioSettings settings = selected();
    Gson gson = new Gson();
    AudioSettings restored = gson.fromJson(gson.toJson(settings), AudioSettings.class);
    MusicQueue requests = gson.fromJson(gson.toJson(queue), MusicQueue.class);
    restored.validate(); requests.validate();
    assertEquals(List.of("music:missing", "music:a", "music:missing"), requests.tracks);
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.world(restored).tracks());
    assertThrows(IllegalArgumentException.class, () -> new MusicPlaylist("id", "Name", requests.tracks, Map.of()));
  }

  @Test void incompleteSelectionsAndDuplicateIdentifiersAreRejected() {
    for (String fields : List.of("", ",\"groupVolumes\":{}", ",\"tracks\":[]")) {
      MusicPlaylist incomplete = new Gson().fromJson("{\"id\":\"one\",\"name\":\"One\"" + fields + "}", MusicPlaylist.class);
      AudioSettings settings = new AudioSettings(); settings.playlists.add(incomplete);
      assertThrows(IllegalArgumentException.class, settings::validate);
      assertThrows(IllegalArgumentException.class, () -> MusicPlaylist.loadMenu(settings, "one"));
      assertEquals(MusicPlaylist.SAME_AS_WORLD, settings.menuPlaylist);
    }
    AudioSettings settings = selected();
    settings.playlists.add(new MusicPlaylist("one", "Duplicate", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, settings::validate);
  }
  @Test void favoritesIsReservedOrderedAndReadsCurrentMembershipAndOwnVolumes() {
    AudioSettings settings = selected();
    settings.favourites.addAll(List.of("music:z", "music:missing", "music:a"));
    settings.favoritesGroupVolumes.put("Alpha", 23);
    MusicPlaylist.load(settings, MusicPlaylist.FAVORITES);
    MusicPlaylist.loadMenu(settings, MusicPlaylist.FAVORITES);
    assertEquals(List.of("music:z", "music:missing", "music:a"), MusicPlaylist.rotation(settings, false));
    assertEquals(23, MusicPlaylist.world(settings).groupVolumes().get("Alpha"));
    settings.favourites.remove("music:missing");
    settings.favourites.add("music:missing");
    assertEquals(List.of("music:z", "music:a", "music:missing"), MusicPlaylist.menu(settings).tracks());
    AudioSettings restored = new Gson().fromJson(new Gson().toJson(settings), AudioSettings.class);
    restored.validate();
    assertEquals(MusicPlaylist.world(settings).tracks(), MusicPlaylist.world(restored).tracks());
    assertEquals(23, MusicPlaylist.menu(restored).groupVolumes().get("Alpha"));
    settings.favourites.clear();
    assertTrue(MusicPlaylist.rotation(settings, false).isEmpty());
    settings.playlists.add(new MusicPlaylist(MusicPlaylist.FAVORITES, "Fake", List.of(), Map.of()));
    assertThrows(IllegalArgumentException.class, settings::validate);
  }

  @Test void favoriteMenuSelectionUsesOnlyFavoritesAndItsOwnGains() {
    AudioSettings settings = selected();
    settings.favourites.addAll(List.of("music:a", "music:favorite"));
    settings.favoritesGroupVolumes.put("Alpha", 12);
    MusicPlaylist.loadMenu(settings, MusicPlaylist.FAVORITES);
    assertEquals(List.of("music:a", "music:favorite"), MusicPlaylist.rotation(settings, true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:favorite", true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    assertEquals(12, MusicPlaylist.menu(settings).groupVolumes().get("Alpha"));
    assertEquals(40, MusicPlaylist.world(settings).groupVolumes().get("Alpha"));
  }

  @Test void volumeSettingsKeepUnavailablePlaylistFavoriteAndOverrideReferences() {
    AudioSettings settings = selected();
    settings.playlists.add(new MusicPlaylist("other", "Other", List.of("music:other", "music:z"), Map.of()));
    settings.favourites.addAll(List.of("music:favorite", "music:z"));
    settings.sounds.put("music:retained", 42);
    settings.sounds.put("random.click", 55);
    List<String> playlistBefore = MusicPlaylist.world(settings).tracks();
    List<String> favoritesBefore = List.copyOf(settings.favourites);
    List<String> ids = settings.musicTrackIds(List.of("music:available", "music:z"));
    assertEquals("music:available", ids.get(0));
    assertTrue(ids.containsAll(List.of("music:missing", "music:other", "music:favorite", "music:retained")));
    assertTrue(ids.containsAll(BuiltinMusic.VANILLA));
    assertEquals(ids.size(), new HashSet<>(ids).size());
    assertFalse(ids.contains("random.click"));
    assertEquals(playlistBefore, MusicPlaylist.world(settings).tracks());
    assertEquals(favoritesBefore, List.copyOf(settings.favourites));
    assertEquals(42, settings.sounds.get("music:retained"));
    assertThrows(UnsupportedOperationException.class, () -> ids.add("music:mutated"));
  }

  @Test void playbackLookupsReadLiveFavoritesWithoutReplacingSnapshotsOrPhysicalGainKeys() {
    AudioSettings settings = selected();
    String physicalFolder = "folder:/music/album";
    settings.favourites.add("music:custom/missing");
    settings.favoritesGroupVolumes.put(physicalFolder, 24);
    settings.favoritesGroupVolumes.put("Alpha", 31);
    settings.libraryOrder.add("source:/music");
    MusicPlaylist.load(settings, MusicPlaylist.FAVORITES);
    MusicPlaylist snapshot = MusicPlaylist.find(settings, MusicPlaylist.FAVORITES);
    assertTrue(MusicPlaylist.contains(settings, MusicPlaylist.SAME_AS_WORLD, "music:custom/missing"));
    assertSame(settings.favoritesGroupVolumes, MusicPlaylist.groupVolumes(settings, MusicPlaylist.SAME_AS_WORLD));
    assertEquals(.24f, MusicGroups.gain(MusicPlaylist.groupVolumes(settings, settings.playlist), physicalFolder), .00001f);
    assertEquals(1f, MusicGroups.gain(MusicPlaylist.groupVolumes(settings, settings.playlist), "source:/music"));
    settings.favourites.remove("music:custom/missing");
    settings.favourites.add("music:custom/new");
    settings.favoritesGroupVolumes.put(physicalFolder, 58);
    assertFalse(MusicPlaylist.contains(settings, settings.playlist, "music:custom/missing"));
    assertTrue(MusicPlaylist.contains(settings, settings.playlist, "music:custom/new"));
    assertEquals(.58f, MusicGroups.gain(MusicPlaylist.groupVolumes(settings, settings.playlist), physicalFolder), .00001f);
    assertEquals(List.of("music:custom/missing"), snapshot.tracks());
    assertEquals(24, snapshot.groupVolumes().get(physicalFolder));
    assertEquals(List.of("music:custom/new"), MusicPlaylist.find(settings, settings.playlist).tracks());
  }

  @Test void directPlaybackLookupsResolveMenuGainsAndSharedWorldSelection() {
    AudioSettings settings = selected();
    settings.favourites.addAll(List.of("music:a", "music:favorite"));
    settings.favoritesGroupVolumes.put("Alpha", 12);
    MusicPlaylist.loadMenu(settings, MusicPlaylist.FAVORITES);
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:favorite", true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:favorite", false));
    assertEquals(12, MusicPlaylist.groupVolumes(settings, settings.menuPlaylist).get("Alpha"));
    assertEquals(40, MusicPlaylist.groupVolumes(settings, settings.playlist).get("Alpha"));
    MusicPlaylist.load(settings, MusicPlaylist.FAVORITES);
    MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD);
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    assertSame(settings.favoritesGroupVolumes, MusicPlaylist.groupVolumes(settings, settings.menuPlaylist));
    assertTrue(MusicPlaylist.contains(settings, settings.menuPlaylist, "music:a"));
    assertFalse(MusicPlaylist.contains(settings, settings.menuPlaylist, "music:z"));
  }

}
