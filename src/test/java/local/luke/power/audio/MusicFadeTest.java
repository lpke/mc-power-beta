package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicFadeTest {
  @Test void zeroDisablesOpeningAndEndingEnvelope() {
    assertEquals(1, MusicFade.gain(0, 0, 10));
    assertEquals(1, MusicFade.gain(0, 10, 10));
  }
  @Test void fadesBothEndsWithoutChangingMiddleGain() {
    assertEquals(0, MusicFade.gain(2000, 0, 10));
    assertEquals(.5f, MusicFade.gain(2000, 1, 10));
    assertEquals(1, MusicFade.gain(2000, 5, 10));
    assertEquals(.5f, MusicFade.gain(2000, 9, 10));
    assertEquals(0, MusicFade.gain(2000, 11, 10));
  }
  @Test void shortAndUnknownDurationsStayBounded() {
    assertEquals(.2f, MusicFade.gain(5000, 1, 2));
    assertEquals(.2f, MusicFade.gain(5000, 1, 0));
    assertEquals(1, MusicFade.gain(5000, 10, 0));
  }
}
