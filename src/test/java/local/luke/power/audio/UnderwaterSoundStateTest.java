package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;

class UnderwaterSoundStateTest {
  @Test void transitionsAreEyeCrossingsAndSpectatorChangesStillUpdateTheBaseline() {
    var state = new UnderwaterSoundState();
    assertEquals(0, state.transition(false, false));
    assertEquals(1, state.transition(true, false));
    assertEquals(0, state.transition(true, false));
    assertEquals(-1, state.transition(false, false));
    assertEquals(0, state.transition(true, true));
    assertEquals(0, state.transition(true, false));
    assertEquals(-1, state.transition(false, false));
  }

  @Test void everyFadeTickAndStopBoundaryMatchesModernIncludingRapidReentry() {
    for (int underwaterTicks = 1; underwaterTicks <= 80; underwaterTicks++) {
      var fade = new UnderwaterSoundState.Fade();
      int reference = 0;
      for (int tick = 0; tick < 160; tick++) {
        boolean wet = tick < underwaterTicks || tick >= underwaterTicks + 6 && tick < underwaterTicks + 16;
        assertEquals(reference < 0, fade.stopped());
        if (reference < 0) break;
        reference = Math.min(reference + (wet ? 1 : -2), 40);
        assertEquals(Math.max(0, Math.min(1, reference / 40f)), fade.tick(wet));
      }
    }
  }

  @Test void strictExclusiveProbabilityBandsMatchModernWithoutRenormalizingDisabledBands() {
    String prefix = UnderwaterSoundState.PREFIX + "loop.additions";
    assertEquals(prefix + ".ultra_rare", UnderwaterSoundState.addition(0));
    assertEquals(prefix + ".ultra_rare", UnderwaterSoundState.addition(Math.nextDown(.0001f)));
    assertEquals(prefix + ".rare", UnderwaterSoundState.addition(.0001f));
    assertEquals(prefix + ".rare", UnderwaterSoundState.addition(Math.nextDown(.001f)));
    assertEquals(prefix, UnderwaterSoundState.addition(.001f));
    assertEquals(prefix, UnderwaterSoundState.addition(Math.nextDown(.01f)));
    assertNull(UnderwaterSoundState.addition(.01f));
    var settings = new AudioSettings(); settings.underwaterAdditions = true;
    assertFalse(UnderwaterSoundState.enabled(settings, UnderwaterSoundState.addition(0)));
    assertTrue(UnderwaterSoundState.enabled(settings, UnderwaterSoundState.addition(.005f)));
  }

  @Test void independentPreferencesRoundTripAndOfficialGainsUseAmbientControls() throws Exception {
    var gson = new Gson(); var settings = gson.fromJson("{}", AudioSettings.class);
    assertFalse(UnderwaterSoundState.any(settings));
    for (String key : List.of("underwaterEnterSounds", "underwaterExitSounds", "underwaterAmbience",
        "underwaterAdditions", "underwaterRareAdditions", "underwaterUltraRareAdditions")) {
      settings = gson.fromJson("{}", AudioSettings.class);
      AudioSettings.class.getField(key).setBoolean(settings, true);
      var restored = gson.fromJson(gson.toJson(settings), AudioSettings.class);
      String id = ExtraSounds.previewId("audio." + key);
      assertTrue(UnderwaterSoundState.enabled(restored, id));
      restored.master = 50; restored.categories.put("ambient", 60); restored.sounds.put(id, 25);
      assertEquals(.075, restored.gain(id, false), 1e-8);
      var variant = WaterSoundCatalog.choose(id, new Random(0));
      assertNotNull(variant); assertTrue(variant.durationTicks() > 0);
      assertEquals(key.equals("underwaterAmbience"), variant.stream());
      if (key.equals("underwaterEnterSounds")) assertEquals(.5f, variant.volume());
      else if (key.equals("underwaterExitSounds")) assertEquals(.3f, variant.volume());
      else if (key.equals("underwaterAmbience")) assertEquals(.65f, variant.volume());
      else assertTrue(variant.volume() >= .45f && variant.volume() <= 1);
    }
  }
}
