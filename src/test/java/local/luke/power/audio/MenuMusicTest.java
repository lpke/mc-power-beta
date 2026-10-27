package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class MenuMusicTest {
  @Test void menusDefaultToTheWorldPlaylist() {
    AudioSettings settings = new Gson().fromJson("{}", AudioSettings.class);
    assertEquals(MusicPlaylist.SAME_AS_WORLD, settings.menuPlaylist);
    assertEquals(MusicPlaylist.rotation(settings, false), MusicPlaylist.rotation(settings, true));
    var serialized = new Gson().toJsonTree(settings).getAsJsonObject();
    assertFalse(serialized.has("menuMusic"));
    assertFalse(serialized.has("recursive"));
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
  @Test void emptyCustomMenuPlaylistNeverFallsBackToWorldMusic() {
    AudioSettings settings = new AudioSettings();
    settings.playlists.add(new MusicPlaylist("empty", "Empty", List.of()));
    MusicPlaylist.loadMenu(settings, "empty");
    assertTrue(MusicPlaylist.rotation(settings, true).isEmpty());
    assertEquals(BuiltinMusic.VANILLA, MusicPlaylist.rotation(settings, false));
  }
}
