package local.luke.power.autowalk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReachDistanceTest {
  @Test void usesClosestFaceNotCentreAndWorksInEveryDirection() {
    assertEquals(0,ReachDistance.toBox(.5,.5,.5,0,0,0,1,1,1));
    assertEquals(16,ReachDistance.toBox(5,.5,.5,0,0,0,1,1,1));
    assertEquals(16,ReachDistance.toBox(-4,.5,.5,0,0,0,1,1,1));
    assertEquals(16,ReachDistance.toBox(.5,5,.5,0,0,0,1,1,1));
    assertEquals(32,ReachDistance.toBox(5,5,.5,0,0,0,1,1,1));
    assertEquals(Double.POSITIVE_INFINITY,ReachDistance.toBox(Double.NaN,0,0,0,0,0,1,1,1));
  }
}
