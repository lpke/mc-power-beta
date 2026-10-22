package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FluidSoundMathTest {
  @Test void nativeVelocityComponentsSetModernSwimmingAndSplashGain() {
    assertEquals(0, FluidSoundMath.volume(0, 0, 0, .35f));
    assertEquals(Math.sqrt(.2) * .35, FluidSoundMath.volume(1, 0, 0, .35f), 1e-7);
    assertEquals(.35, FluidSoundMath.volume(0, 1, 0, .35f), 1e-7);
    assertEquals(1, FluidSoundMath.volume(0, 100, 0, .35f));
    assertEquals(0, FluidSoundMath.volume(Double.NaN, 0, 0, .35f));
    assertEquals(FluidSoundMath.volume(-1, -2, -3, .2f), FluidSoundMath.volume(1, 2, 3, .2f));
  }
  @Test void highSpeedSplashUsesTheExactModernThreshold() {
    assertEquals("power_beta:entity.player.splash", FluidSoundMath.splash(Math.nextDown(.25f)));
    assertEquals("power_beta:entity.player.splash.high_speed", FluidSoundMath.splash(.25f));
  }
  @Test void travelUsesHorizontalDistanceAndSkipsMissedThresholds() {
    assertEquals(3, FluidSoundMath.distance(3, 4), 1e-10);
    assertEquals(1, FluidSoundMath.nextStep(0));
    assertEquals(4, FluidSoundMath.nextStep(3.8));
    assertEquals(5, FluidSoundMath.nextStep(4));
  }
}
