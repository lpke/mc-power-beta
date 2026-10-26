package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class MenuMusicTest {
  @Test void newSettingsMixBothAndOldExplicitChoicesSurvive() {
    Gson json = new Gson();
    assertEquals(AudioSettings.MenuMusic.MIX, json.fromJson("{}", AudioSettings.class).menuMusic);
    assertEquals(AudioSettings.MenuMusic.WORLD,
        json.fromJson("{\"menuMusic\":\"WORLD\"}", AudioSettings.class).menuMusic);
  }
  @Test void titleControlsRespectMasterAndTheIndependentTitleSwitch() {
    AudioSettings s = new AudioSettings();
    assertTrue(s.menuControlsMainMenu);
    assertTrue(s.showMenuControls(false));
    assertTrue(s.showMenuControls(true));
    s.menuControls = false;
    assertFalse(s.showMenuControls(false));
    assertFalse(s.showMenuControls(true));
    s.menuControls = true;
    assertTrue(s.showMenuControls(false));
    assertTrue(s.showMenuControls(true));
    s.menuControlsMainMenu = false;
    assertTrue(s.showMenuControls(false));
    assertFalse(s.showMenuControls(true));
  }
  @Test void onlySeparateMenuSelectionResetsPlaybackOnWorldTransitions() {
    AudioSettings s = new AudioSettings();
    for (var mode : AudioSettings.MenuMusic.values()) {
      s.menuMusic = mode;
      assertEquals(mode != AudioSettings.MenuMusic.CUSTOM, s.continuesAcrossMenus());
    }
  }
  @Test void emptyCustomMenuPlaylistNeverFallsBackToWorldMusic() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of(), Map.of()));
    MusicPlaylist.loadMenu(settings, "empty");
    settings.menuMusic = AudioSettings.MenuMusic.CUSTOM;
    assertTrue(MusicPlaylist.rotation(settings, true).isEmpty());
    settings.menuMusic = AudioSettings.MenuMusic.MIX;
    assertEquals(BuiltinMusic.VANILLA, MusicPlaylist.rotation(settings, true));
  }
}
