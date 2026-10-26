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
    assertEquals(List.of(MusicPlaylist.beta()), MusicPlaylist.all(settings));
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

  @Test void menuModesUseSavedOrderDeduplicateMixAndSelectTheMatchingGains() {
    AudioSettings settings = selected();
    settings.playlists.add(new MusicPlaylist("menu", "Menu", List.of("music:a", "music:menu"), Map.of("Alpha", 20)));
    MusicPlaylist.loadMenu(settings, "menu");
    settings.menuMusic = AudioSettings.MenuMusic.WORLD;
    assertEquals(MusicPlaylist.world(settings).tracks(), MusicPlaylist.rotation(settings, true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:menu", true));
    settings.menuMusic = AudioSettings.MenuMusic.MIX;
    assertEquals(List.of("music:z", "music:missing", "music:a", "music:menu"), MusicPlaylist.rotation(settings, true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:menu", true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    settings.menuMusic = AudioSettings.MenuMusic.CUSTOM;
    assertEquals(List.of("music:a", "music:menu"), MusicPlaylist.rotation(settings, true));
    assertTrue(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
    assertFalse(settings.continuesAcrossMenus());
    MusicPlaylist.loadMenu(settings, MusicPlaylist.SAME_AS_WORLD);
    assertEquals(MusicPlaylist.world(settings).tracks(), MusicPlaylist.rotation(settings, true));
    assertFalse(MusicPlaylist.usesMenuVolumes(settings, "music:a", true));
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
}
