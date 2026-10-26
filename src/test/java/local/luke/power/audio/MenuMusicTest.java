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
  @Test void onlySeparateMenuFoldersResetPlaybackOnWorldTransitions() {
    AudioSettings s = new AudioSettings();
    for (var mode : AudioSettings.MenuMusic.values()) {
      s.menuMusic = mode;
      assertEquals(mode != AudioSettings.MenuMusic.CUSTOM, s.continuesAcrossMenus());
    }
  }
  @Test void excludedDedicatedMenuTracksDoNotRestoreWorldFallback() {
    List<String> world = List.of("calm1.ogg"), menu = List.of("menu.ogg");
    Set<String> excluded = Set.of("music:menu.ogg");
    assertTrue(MusicRotation.menu(world, menu, true,
        track -> !excluded.contains("music:" + track)).isEmpty());
    // Active ignores exclusions, but must agree that the dedicated menu replaces world music.
    assertEquals(menu, MusicRotation.menu(world, menu, true, track -> true));
  }

  @Test void contextIneligibleDedicatedMenuTracksDoNotRestoreWorldFallback() {
    List<String> world = List.of("calm1.ogg");
    List<String> menu = List.of("song-nether-specific.ogg");
    assertTrue(MusicRotation.menu(world, menu, true,
        track -> TrackRules.eligible(track, 0, "Forest")).isEmpty());
    assertEquals(menu, MusicRotation.menu(world, menu, true,
        track -> TrackRules.eligible(track, -1, "Hell")));
  }

  @Test void missingDedicatedMenuSourcesKeepFallbackAndMixKeepsWorldTracks() {
    List<String> world = List.of("calm1.ogg");
    assertEquals(world, MusicRotation.menu(world, List.<String>of(), true, track -> true));
    assertEquals(world, MusicRotation.menu(world, List.of("excluded.ogg"), false, track -> false));
    assertEquals(List.of("calm1.ogg", "first.ogg", "second.ogg"),
        MusicRotation.menu(world, List.of("first.ogg", "second.ogg"), false, track -> true));
  }

}
