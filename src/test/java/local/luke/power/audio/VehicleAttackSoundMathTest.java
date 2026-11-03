package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VehicleAttackSoundMathTest {
  @Test void modernChargeChoosesSoundWithoutChangingNativeDamageTiming() {
    assertEquals("entity.player.attack.strong", VehicleAttackSoundMath.hit(25, 0, false, true, 0));
    assertEquals("entity.player.attack.weak", VehicleAttackSoundMath.hit(0, 0, false, true, 0));
    assertFalse(VehicleAttackSoundMath.charged(4, 0));
    assertTrue(VehicleAttackSoundMath.charged(5, 0));
    assertFalse(VehicleAttackSoundMath.charged(10, 267));
    assertTrue(VehicleAttackSoundMath.charged(11, 267));
    assertEquals("entity.player.attack.sweep", VehicleAttackSoundMath.hit(12, 267, false, true, 0));
    assertEquals("entity.player.attack.strong", VehicleAttackSoundMath.hit(12, 267, false, false, 0));
    assertEquals("entity.player.attack.strong", VehicleAttackSoundMath.hit(12, 267, true, true, 0));
    double threshold = (double) .1f * 2.5;
    assertEquals("entity.player.attack.sweep", VehicleAttackSoundMath.hit(12, 267, false, true, .25 * .25));
    assertEquals("entity.player.attack.sweep", VehicleAttackSoundMath.hit(12, 267, false, true, Math.nextDown(threshold * threshold)));
    assertEquals("entity.player.attack.strong", VehicleAttackSoundMath.hit(12, 267, false, true, threshold * threshold));
    assertEquals("entity.player.attack.strong", VehicleAttackSoundMath.hit(12, 267, false, true, Math.nextUp(threshold * threshold)));
  }

  @Test void betaToolsRetainModernSoundChargeDelaysAndAllSwordMaterials() {
    for (int item : new int[]{268, 272, 267, 276, 283}) {
      assertTrue(VehicleAttackSoundMath.sword(item)); assertEquals(12.5f, VehicleAttackSoundMath.delay(item));
    }
    for (int item : new int[]{269, 273, 256, 277, 284, 290, 294, 279, 286}) assertEquals(20, VehicleAttackSoundMath.delay(item));
    for (int item : new int[]{270, 274, 257, 278, 285}) assertEquals(20 / 1.2f, VehicleAttackSoundMath.delay(item));
    assertEquals(25, VehicleAttackSoundMath.delay(271)); assertEquals(25, VehicleAttackSoundMath.delay(275));
    assertEquals(20 / .9f, VehicleAttackSoundMath.delay(258));
    assertEquals(10, VehicleAttackSoundMath.delay(291)); assertEquals(20 / 3f, VehicleAttackSoundMath.delay(292));
    assertEquals(5, VehicleAttackSoundMath.delay(293));
    assertEquals(5, VehicleAttackSoundMath.delay(9999));
  }
}
