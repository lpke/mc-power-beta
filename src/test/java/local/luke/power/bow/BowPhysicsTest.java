package local.luke.power.bow;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BowPhysicsTest {
  @Test void officialChargeBoundariesAndPullFrames() {
    assertEquals(0,BowPhysics.power(0));
    assertTrue(BowPhysics.power(2)<.1f);
    assertEquals(.1075f,BowPhysics.power(3),1e-7);
    assertEquals(5f/12,BowPhysics.power(10),1e-7);
    assertEquals(1,BowPhysics.power(20));assertEquals(1,BowPhysics.power(72000));
    assertEquals(0,BowPhysics.texture(12.99f));assertEquals(1,BowPhysics.texture(13));
    assertEquals(1,BowPhysics.texture(17.99f));assertEquals(2,BowPhysics.texture(18));
  }
  @Test void officialUnenchantedDamageUsesFloatSpeedAndCriticalInclusiveRange() {
    assertEquals(6,BowPhysics.damage(0,0,3));
    assertEquals(3,BowPhysics.damage(0,0,1.25));
    assertEquals(2,BowPhysics.damage(0,0,.75));
    assertEquals(4,BowPhysics.damage(1,1,1));
    assertEquals(5,BowPhysics.criticalBound(6)); // A full-speed critical does 6..10 damage.
    assertEquals(384,BowPhysics.DURABILITY);
  }
  @Test void triangularSpreadAndReleasePitchMatchOfficialEndpoints() {
    assertEquals(0,BowPhysics.triangle(.5,.5));
    assertEquals(.0172275,BowPhysics.triangle(1,0),1e-15);
    assertEquals(-.0172275,BowPhysics.triangle(0,1),1e-15);
    assertEquals(1.3333333,BowPhysics.shotPitch(0,1),1e-6);
    assertEquals(1.125,BowPhysics.shotPitch(1,1),1e-6);
    assertEquals(.05,BowPhysics.GRAVITY);
    assertEquals((double).99f,BowPhysics.AIR_DRAG);
    assertEquals((double).6f,BowPhysics.WATER_DRAG);
  }
}
