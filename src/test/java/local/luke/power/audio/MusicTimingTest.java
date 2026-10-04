package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicTimingTest {
  @Test void queueDelayCannotOverrideTheMasterWaitSwitch() {
    for (boolean queued : new boolean[]{false, true})
      for (boolean delay : new boolean[]{false, true})
        assertEquals(0, MusicTiming.remaining(false, delay, queued, 1200));
    assertEquals(1200, MusicTiming.remaining(true, true, true, 1200));
    assertEquals(0, MusicTiming.remaining(true, false, true, 1200));
    assertEquals(1200, MusicTiming.remaining(true, false, false, 1200));
    assertEquals(0, MusicTiming.remaining(true, true, true, -1));
  }

  @Test void remainingTimeUsesReadableUnitsWithoutClaimingAnImmediateStart() {
    assertEquals("Not playing. Next track shortly.", MusicTiming.status(0));
    assertEquals("Not playing. 1 sec until next track.", MusicTiming.status(1));
    assertEquals("Not playing. 60 sec until next track.", MusicTiming.status(1200));
    assertEquals("Not playing. 61 sec until next track.", MusicTiming.status(1201));
  }

  @Test void oldConfigsAcquireTheNewDefaultsAndNewValuesRoundTrip() {
    var gson = new com.google.gson.Gson();
    var old = gson.fromJson("{}", AudioSettings.class);
    assertTrue(old.waitBetweenTracks);
    assertFalse(old.delayQueuedTracks);
    old.waitBetweenTracks = false; old.delayQueuedTracks = true;
    var restored = gson.fromJson(gson.toJson(old), AudioSettings.class);
    assertFalse(restored.waitBetweenTracks);
    assertTrue(restored.delayQueuedTracks);
  }

  @Test void builtInTitlesDoNotRenameUnknownFiles() {
    assertEquals("calm1.ogg (Minecraft)", BuiltinMusic.label("calm1.ogg"));
    assertEquals("nuance2.ogg (Oxygène)", BuiltinMusic.label("nuance2.ogg"));
    assertEquals("piano3.mus (Mice on Venus)", BuiltinMusic.label("piano3.mus"));
    assertEquals("my-calm1.ogg", BuiltinMusic.label("my-calm1.ogg"));
  }
}
