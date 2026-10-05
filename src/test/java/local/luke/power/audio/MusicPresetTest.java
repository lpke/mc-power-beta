package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.*;
import local.luke.power.ui.MusicPresetDraft;
import org.junit.jupiter.api.Test;

class MusicPresetTest {
  @Test void newMusicStaysOutOfSavedAndLoadedPresetsUntilExplicitlyIncluded() {
    String old = "music:custom/old", added = "music:custom/new";
    MusicPresetDraft create = new MusicPresetDraft(null, Set.of(old));
    MusicPreset saved = create.snapshot("My songs");
    AudioSettings live = new AudioSettings();
    live.presets.add(saved);
    MusicPreset.load(live, saved.id());
    assertFalse(saved.includes(added));
    assertFalse(live.presetTrackPool.contains(added));
    MusicPresetDraft editor = new MusicPresetDraft(saved, Set.of(old, added));
    assertFalse(editor.included(added));
    assertFalse(editor.changed(saved.name()));
    editor.include(added, true);
    assertTrue(editor.snapshot(saved.name()).includes(added));
    assertFalse(saved.includes(added));
    assertFalse(live.presetTrackPool.contains(added));
    live.presetTrackPool.add(added);
    MusicPreset.load(live, saved.id());
    assertFalse(live.presetTrackPool.contains(added));
    Gson gson = new Gson();
    live = gson.fromJson(gson.toJson(live), AudioSettings.class);
    live.validate();
    assertEquals(Set.of(old), live.presetTrackPool);
    assertFalse(live.presets.get(0).includes(added));
    editor.includeAll();
    assertEquals(Set.of(old, added), editor.snapshot(saved.name()).trackPool());
  }
  @Test void groupVolumesAreIsolatedPersistedAndOnlyCopiedWhenLoaded() {
    AudioSettings live = new AudioSettings();
    live.groupVolumes.put("Alpha", 75);
    MusicPreset saved = new MusicPreset("id", "Name", Set.of(), Map.of("Alpha", 40));
    MusicPresetDraft draft = new MusicPresetDraft(saved);
    draft.volume("Alpha", 20);
    draft.volume("folder:/music", 0);
    assertEquals(40, saved.groupVolumes().get("Alpha"));
    assertEquals(75, live.groupVolumes.get("Alpha"));
    assertTrue(draft.changed("Name"));
    live.presets.add(draft.snapshot("Name"));
    Gson gson = new Gson();
    live = gson.fromJson(gson.toJson(live), AudioSettings.class);
    live.validate();
    MusicPreset.load(live, "id");
    assertEquals(Map.of("Alpha", 20, "folder:/music", 0), live.groupVolumes);
    assertEquals(0.2f, MusicGroups.gain(live.groupVolumes, "Alpha"), 0.0001f);
    assertEquals(1f, MusicGroups.gain(live.groupVolumes, "Unknown"));
    live.groupVolumes.put("Alpha", 100);
    assertEquals(20, live.presets.get(0).groupVolumes().get("Alpha"));
    MusicPreset.load(live, "");
    assertEquals(100, live.groupVolumes.get("Alpha"));
    assertThrows(IllegalArgumentException.class, () -> draft.volume("Alpha", -1));
    assertEquals(20, draft.volume("Alpha"));
    draft.volume("Alpha", 100);
    assertFalse(draft.snapshot("Name").groupVolumes().containsKey("Alpha"));
  }

  @Test void legacyPresetsUseFullGroupVolumeAndInvalidVolumesDoNotPartlyLoad() {
    Gson gson = new Gson();
    var legacy = gson.fromJson("{\"id\":\"old\",\"name\":\"Old\",\"excluded\":[]}", MusicPreset.class);
    AudioSettings live = new AudioSettings(); live.groupVolumes.put("Alpha", 20);
    live.presets.add(legacy); live.validate(); MusicPreset.load(live, "old");
    assertTrue(live.groupVolumes.isEmpty());
    var bad = gson.fromJson("{\"id\":\"bad\",\"name\":\"Bad\",\"excluded\":[\"music:calm1.ogg\"],\"groupVolumes\":{\"Alpha\":101}}", MusicPreset.class);
    live.presets.add(bad);
    assertThrows(IllegalArgumentException.class, () -> MusicPreset.load(live, "bad"));
    assertTrue(live.disabledTracks.isEmpty()); assertEquals("old", live.preset);
  }

  @Test void bulkDraftEditsRetainMissingSelectionsAndDoNotChangeTheSavedPreset() {
    MusicPreset saved=new MusicPreset("id","Name",Set.of("music:missing.ogg"));
    MusicPresetDraft draft=new MusicPresetDraft(saved);
    List<String> tracks=new ArrayList<>();
    for(int i=0;i<4203;i++)tracks.add("music:track"+i+".ogg");
    draft.excludeAll(tracks);
    assertEquals(4204,draft.snapshot("Name").excluded().size());
    assertEquals(Set.of("music:missing.ogg"),saved.excluded());
    assertThrows(IllegalArgumentException.class,()->draft.excludeAll(List.of("invalid")));
    assertEquals(4204,draft.snapshot("Name").excluded().size());
    draft.includeAll();
    assertTrue(draft.snapshot("Name").excluded().isEmpty());
  }

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
