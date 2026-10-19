package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SprintCollisionTest {
  @Test void glancingContactSurvivesButHeadOnAndBlockedMovementStop() {
    assertTrue(SprintCollision.minor(.1f,1,0,0,.2));
    assertFalse(SprintCollision.minor(.2f,1,0,0,.2));
    assertFalse(SprintCollision.minor(0,1,0,0,0));
    assertFalse(SprintCollision.minor(0,1,0,.2,0));
    assertFalse(SprintCollision.minor(0,0,0,.2,0));
  }
  @Test void bearingAndScaleDoNotChangeTheOfficialEightDegreeBoundary() {
    for(float yaw:new float[]{0,45,90,180,270})for(double speed:new double[]{.05,.2,.5}) {
      double radians=Math.toRadians(yaw);
      assertTrue(SprintCollision.minor(.1f,1,yaw,-Math.sin(radians)*speed,Math.cos(radians)*speed));
      assertFalse(SprintCollision.minor(.2f,1,yaw,-Math.sin(radians)*speed,Math.cos(radians)*speed));
    }
  }
}
