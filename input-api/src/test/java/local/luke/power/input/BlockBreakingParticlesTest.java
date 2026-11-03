package local.luke.power.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockBreakingParticlesTest {
  @AfterEach void restoreState() {
    BlockBreakingParticles.reset();
    BlockBreakingParticles.configure(() -> true, () -> false);
    TweakIndicators.register(TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES, () -> null);
  }

  @Test void temporaryInversionComposesWithBothPreferencesAndGlobalSuppression() {
    for (boolean saved : new boolean[]{false, true}) for (boolean allDisabled : new boolean[]{false, true}) {
      BlockBreakingParticles.reset();
      assertEquals(!allDisabled && saved, BlockBreakingParticles.enabled(saved, allDisabled));
      BlockBreakingParticles.toggle();
      assertTrue(BlockBreakingParticles.inverted());
      assertEquals(!allDisabled && !saved, BlockBreakingParticles.enabled(saved, allDisabled));
      BlockBreakingParticles.toggle();
      assertFalse(BlockBreakingParticles.inverted());
      assertEquals(!allDisabled && saved, BlockBreakingParticles.enabled(saved, allDisabled));
    }
  }

  @Test void livePreferenceEditsStayIndependentAndWorldExitRestoresTheLatestSavedValue() {
    boolean[] saved = {true}, allDisabled = {false};
    BlockBreakingParticles.configure(() -> saved[0], () -> allDisabled[0]);
    TweakLifecycle.register("block-breaking-particles-test", BlockBreakingParticles::reset);
    BlockBreakingParticles.toggle();
    assertFalse(BlockBreakingParticles.enabled());
    assertTrue(saved[0]);
    saved[0] = false;
    assertTrue(BlockBreakingParticles.enabled());
    allDisabled[0] = true;
    assertFalse(BlockBreakingParticles.enabled());
    TweakLifecycle.reset();
    assertFalse(BlockBreakingParticles.inverted());
    assertFalse(saved[0]);
    assertTrue(allDisabled[0]);
    allDisabled[0] = false;
    assertFalse(BlockBreakingParticles.enabled());
    saved[0] = true;
    assertTrue(BlockBreakingParticles.enabled());
  }

  @Test void indicatorReportsEffectiveStateOnlyDuringTheOverride() {
    boolean[] allDisabled = {false};
    BlockBreakingParticles.configure(() -> false, () -> allDisabled[0]);
    var tweak = TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES;
    TweakIndicators.register(tweak, () -> BlockBreakingParticles.inverted()
        ? BlockBreakingParticles.enabled() ? "enabled" : "disabled" : null);
    assertNull(TweakIndicators.label(tweak));
    BlockBreakingParticles.toggle();
    assertEquals("Block breaking particles enabled", TweakIndicators.label(tweak));
    allDisabled[0] = true;
    assertEquals("Block breaking particles disabled", TweakIndicators.label(tweak));
    BlockBreakingParticles.reset();
    assertNull(TweakIndicators.label(tweak));
  }
}
