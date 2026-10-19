package local.luke.power.validation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MapPanFramesTest {
  @Test void nativeFramePanningIsDistinctFromTwentyHertzSimulation() {
    var recorder = new MapPanFrames.Recorder(1024); Object owner = new Object(); recorder.start(owner, 0);
    for (int frame = 0; frame < 120; frame++) {
      if (frame % 3 == 0) recorder.tick();
      recorder.frame(owner, frame, 0, true, 1_000_000_000L * frame / 60);
    }
    var result = recorder.finish(2_000_000_000L);
    assertEquals(60, result.viewChangesPerSecond(), .001);
    assertEquals(60, result.renderFps(), .001);
    assertEquals(20, result.nativeTicksPerSecond(), .5);
    assertEquals(119, result.distinctViewChanges());
    assertEquals(16.666667, result.frameP95Ms(), .00001);
  }

  @Test void fastRenderingWithTickBoundViewChangesRemainsDetectable() {
    var recorder = new MapPanFrames.Recorder(1024); Object owner = new Object(); recorder.start(owner, 0);
    for (int frame = 0; frame < 240; frame++) {
      if (frame % 6 == 0) recorder.tick();
      recorder.frame(owner, frame / 6, 0, true, 1_000_000_000L * frame / 120);
    }
    var result = recorder.finish(2_000_000_000L);
    assertEquals(120, result.renderFps(), .001);
    assertTrue(result.viewChangesPerSecond() <= 20);
    assertEquals(50, result.changeMedianMs(), .00001);
  }

  @Test void ringKeepsOnlyLatestSamplesAndIgnoresOtherScreensOrUnheldChanges() {
    var recorder = new MapPanFrames.Recorder(4); Object owner = new Object(); recorder.start(owner, 0);
    for (int frame = 0; frame < 10; frame++) {
      recorder.frame(new Object(), 999, 999, true, frame * 10_000_000L);
      recorder.frame(owner, frame, 0, false, frame * 10_000_000L);
    }
    var result = recorder.finish(100_000_000L);
    assertEquals(10, result.elapsedFrames()); assertEquals(4, result.storedFrames());
    assertEquals(0, result.distinctViewChanges()); assertEquals(0, result.dragFrames());
    assertEquals(.03, result.sampledSeconds(), .000001);
    assertEquals(5, recorder.csv().lines().count());
    recorder.frame(owner, 123, 0, true, 200_000_000L);
    assertEquals(4, recorder.finish(300_000_000L).storedFrames());
  }
}
