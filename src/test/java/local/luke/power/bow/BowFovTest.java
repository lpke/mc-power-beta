package local.luke.power.bow;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BowFovTest {
  @Test void fullDrawAndReleaseUseOfficialHalfResponseAndPartialTicks() {
    var fov = new BowFov();
    fov.tick(20, 1, 1, 1);
    assertEquals(1, fov.factor(0)); assertEquals(.925f, fov.factor(1), 1e-7f);
    fov.tick(20, 1, 1, 1);
    assertEquals(.925f, fov.factor(0), 1e-7f); assertEquals(.90625f, fov.factor(.5f), 1e-7f);
    assertEquals(.8875f, fov.factor(1), 1e-7f);
    fov.tick(-1, 1, 1, 1);
    assertEquals(.8875f, fov.factor(0), 1e-7f); assertEquals(.94375f, fov.factor(1), 1e-7f);
    for (int i = 0; i < 30; i++) fov.tick(-1, 1, 1, 1);
    assertFalse(fov.active()); assertEquals(1, fov.factor(.5f));
  }

  @Test void drawTargetUsesWholeUseTicksBeforeCameraInterpolation() {
    var fov = new BowFov();
    fov.tick(10, 1, 1, 1);
    assertEquals(.98125f, fov.factor(1), 1e-7f);
    assertEquals(.990625f, fov.factor(.5f), 1e-7f);
    fov.reset(); fov.tick(0, 1, 1, 1);
    assertFalse(fov.active()); assertEquals(1, fov.factor(.5f));
  }

  @Test void simultaneousSprintAndDrawSmoothTheCombinedTargetOnce() {
    var fov = new BowFov();
    fov.tick(20, 1.15f, 1, 1.075f);
    assertEquals(1, fov.factor(0));
    // Camera target is 1.15*.85=.9775; half response from1 is .98875.
    assertEquals(.98875f, fov.factor(1) * 1.075f, 1e-7f);
    assertEquals(.994375f, fov.factor(.5f) * 1.0375f, 1e-7f);
    fov.tick(-1, 1, 1.075f, 1.0375f);
    assertEquals(.994375f, fov.factor(1) * 1.0375f, 1e-7f);
  }

  @Test void releasedBowSettlesWithinOneUlpOfSteadySprintAndThenStaysIdle() {
    for (float base : new float[]{1.15f, 1.13f}) {
      var fov = new BowFov();
      float current = base;
      for (int i = 0; i < 25; i++) {
        float previous = current;
        current += (base * .85f - current) * .5f;
        fov.tick(20, base, base, base);
        assertTrue(fov.active());
        assertEquals(previous / base, fov.factor(0));
        assertEquals(current / base, fov.factor(1));
      }
      int releasedTicks = 0;
      while (fov.active() && releasedTicks++ < 48) {
        float previous = current;
        current += (base - current) * .5f;
        fov.tick(-1, base, base, base);
        if (fov.active()) {
          assertEquals(previous / base, fov.factor(0));
          assertEquals(current / base, fov.factor(1));
        } else {
          assertTrue(Math.abs(previous - base) <= Math.ulp(base));
          assertTrue(Math.abs(current - base) <= Math.ulp(base));
        }
      }
      assertFalse(fov.active(), "released bow did not settle at sprint baseline " + base);
      fov.tick(-1, Float.NaN, Float.NaN, Float.NaN);
      assertFalse(fov.active());
      assertEquals(1, fov.factor(Float.NaN, Float.NaN));
    }
  }

  @Test void endingSprintDuringBowReleasePreservesTheResponseUntilBothSamplesSettle() {
    var fov = new BowFov();
    double movement = 1.15;
    float current = (float)movement;
    for (int i = 0; i < 25; i++) {
      current += ((float)movement * .85f - current) * .5f;
      fov.tick(20, (float)movement, (float)movement, (float)movement);
    }
    int releasedTicks = 0;
    while (fov.active() && releasedTicks++ < 48) {
      float basePrevious = (float)movement;
      movement += (1 - movement) * .5;
      if (Math.abs(movement - 1) < 1e-5) movement = 1;
      float baseCurrent = (float)movement, previous = current;
      current += (1 - current) * .5f;
      fov.tick(-1, 1, basePrevious, baseCurrent);
      if (fov.active()) {
        assertEquals(previous / basePrevious, fov.factor(0));
        assertEquals(current / baseCurrent, fov.factor(1));
      } else {
        assertTrue(Math.abs(previous - basePrevious) <= Math.ulp(basePrevious));
        assertTrue(Math.abs(current - baseCurrent) <= Math.ulp(baseCurrent));
      }
    }
    assertFalse(fov.active(), "released bow did not settle after sprint ended");
    assertEquals(1, fov.factor(.5f));
    fov.tick(0, Float.NaN, Float.NaN, Float.NaN);
    assertFalse(fov.active());
    assertEquals(1, fov.factor(Float.NaN, Float.NaN));
  }

  @Test void disabledIdleStateDoesNoInterpolationAndResetClearsBothSamples() {
    var fov = new BowFov();
    fov.tick(-1, Float.NaN, Float.NaN, Float.NaN);
    assertFalse(fov.active()); assertEquals(1, fov.factor(.25f));
    fov.tick(20, 1, 1, 1); assertTrue(fov.active());
    fov.reset(); assertFalse(fov.active()); assertEquals(1, fov.factor(0)); assertEquals(1, fov.factor(1));
    assertEquals(1, fov.factor(Float.NaN, Float.NaN));
  }

  @Test void frozenCameraSamplesKeepTheirFovWhenTheMovementRendererChanges() {
    var fov = new BowFov();fov.tick(20,1,1,1);
    float frozen=fov.factor(.5f);
    // Pausing does not tick Camera. Any independently updated sprint factor must cancel out.
    assertEquals(frozen,1.1f*fov.factor(.5f,1.1f),1e-7f);
    assertEquals(frozen,.9f*fov.factor(.5f,.9f),1e-7f);
  }
}
