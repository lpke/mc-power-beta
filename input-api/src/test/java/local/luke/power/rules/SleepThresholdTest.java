package local.luke.power.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SleepThresholdTest {
  @Test void zeroStillNeedsOneSleeper() {
    assertFalse(SleepThreshold.reached(1, 0, 0));
    assertFalse(SleepThreshold.reached(0, 0, 0));
    assertTrue(SleepThreshold.reached(10, 1, 0));
  }
  @Test void percentagesRoundUpAndValuesAboveHundredDisableSkipping() {
    assertFalse(SleepThreshold.reached(3, 1, 50));
    assertTrue(SleepThreshold.reached(3, 2, 50));
    assertTrue(SleepThreshold.reached(1, 1, 100));
    assertFalse(SleepThreshold.reached(1, 1, 101));
    assertFalse(SleepThreshold.reached(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertTrue(SleepThreshold.reached(Integer.MAX_VALUE, Integer.MAX_VALUE, 100));
  }
}
