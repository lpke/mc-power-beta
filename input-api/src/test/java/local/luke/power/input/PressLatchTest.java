package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PressLatchTest {
  @Test void repeatedPressNeedsMatchingReleaseEvenIfKeyboardStateWasRecreated() {
    PressLatch latch=new PressLatch();
    assertTrue(latch.claim(87));
    for(int i=0;i<50;i++)assertFalse(latch.claim(87));
    latch.release(29);assertFalse(latch.claim(87));
    latch.release(87);assertTrue(latch.claim(87));
    assertTrue(latch.claim(114));latch.release(87);assertFalse(latch.claim(114));
    latch.release(114);assertTrue(latch.claim(114));
    assertFalse(latch.claim(0));
  }
}
