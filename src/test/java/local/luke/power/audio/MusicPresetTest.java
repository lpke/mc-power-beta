package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import local.luke.power.ui.MusicPresetDraft;
import org.junit.jupiter.api.Test;

class MusicPresetTest {
  @Test void purchasedAlbumExtrasBelongToBothAllModesOnly() {
    var extras = BuiltinMusic.TRACKS.stream().filter(t -> t.usage().equals("Album extras")).toList();
    assertEquals(15, extras.size());
    for (var t : extras) {
      assertTrue(t.included(AudioSettings.MusicMode.ALPHA_BETA));
      assertTrue(t.included(AudioSettings.MusicMode.ALL_MINECRAFT));
      assertFalse(t.included(AudioSettings.MusicMode.VANILLA));
      assertFalse(t.included(AudioSettings.MusicMode.ALPHA_BETA_SURVIVAL));
      assertFalse(t.included(AudioSettings.MusicMode.MINECRAFT_SURVIVAL));
    }
  }

  @Test
  void editingAndLoadingAreSeparateAndMissingTrackSelectionsSurvive() {
    AudioSettings live = new AudioSettings();
    live.disabledTracks.add("music:calm1.ogg");
    MusicPreset original = new MusicPreset("one", "One", Set.of("music:missing.ogg"));
    live.presets.add(original);
    MusicPresetDraft draft = new MusicPresetDraft(original);
    draft.include("music:creative1.ogg", false);
    MusicPreset renamed = draft.snapshot("Renamed");
    assertEquals(Set.of("music:missing.ogg"), original.excluded());
    assertEquals(Set.of("music:calm1.ogg"), live.disabledTracks);
    live.presets.set(0, renamed);
    MusicPreset.load(live, "one");
    assertEquals(Set.of("music:missing.ogg", "music:creative1.ogg"), live.disabledTracks);
    live.disabledTracks.clear();
    assertEquals(2, renamed.excluded().size());
    MusicPreset.load(live, "one");
    assertEquals(2, live.disabledTracks.size());
    MusicPreset.load(live, "");
    assertEquals(2, live.disabledTracks.size());
  }

  @Test
  void persistedDataValidatesWithTheGamesGsonVersion() {
    AudioSettings s = new AudioSettings();
    s.presets.add(new MusicPreset("id", "Name", Set.of("music:calm1.ogg")));
    s.preset = "id";
    s.favourites.add("music:calm2.ogg");
    Gson gson = new Gson();
    AudioSettings restored = gson.fromJson(gson.toJson(s), AudioSettings.class);
    restored.validate();
    assertEquals("Name", restored.presets.get(0).name());
    assertEquals(s.favourites, restored.favourites);
    AudioSettings legacy = gson.fromJson("{\"musicMode\":\"ALPHA_BETA\"}", AudioSettings.class);
    legacy.validate();
    assertEquals(AudioSettings.MusicMode.ALPHA_BETA, legacy.musicMode);
    assertTrue(legacy.preset.isEmpty());
  }

  @Test
  void malformedDataCannotReplaceASelection() {
    assertThrows(IllegalArgumentException.class, () -> new MusicPreset("bad/id", "Bad", Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new MusicPreset("id", " ", Set.of()));
    AudioSettings s = new AudioSettings();
    s.disabledTracks.add("music:calm1.ogg");
    assertThrows(IllegalArgumentException.class, () -> MusicPreset.load(s, "missing"));
    assertEquals(Set.of("music:calm1.ogg"), s.disabledTracks);
    s.presets.add(new MusicPreset("id", "One", Set.of()));
    s.presets.add(new MusicPreset("id", "Two", Set.of()));
    assertThrows(IllegalArgumentException.class, s::validate);
  }

  @Test
  void everyDimensionTrackIsLibraryOnlyForEveryWorldMode() {
    var dimensions =
        BuiltinMusic.TRACKS.stream()
            .filter(t -> Set.of("Nether", "End").contains(t.usage()))
            .toList();
    assertEquals(9, dimensions.size());
    for (var t : dimensions)
      for (var mode : AudioSettings.MusicMode.values()) assertFalse(t.included(mode), t.file());
  }
}
