package local.luke.power.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickMiningTest {
  @Test void overrideAlwaysInvertsCurrentPreferenceAndResetRestoresIt() {
    ClickMining.reset();
    try {
      assertTrue(ClickMining.enabled(true)); assertFalse(ClickMining.enabled(false));
      ClickMining.toggle(); assertTrue(ClickMining.inverted());
      assertFalse(ClickMining.enabled(true)); assertTrue(ClickMining.enabled(false));
      ClickMining.toggle(); assertFalse(ClickMining.inverted());
      ClickMining.toggle(); ClickMining.reset(); assertTrue(ClickMining.enabled(true));
    } finally { ClickMining.reset(); }
  }
  @Test void hudLabelUsesTheEffectiveState() {
    TweakIndicators.register(TweakIndicators.Tweak.CLICK_MINING, () -> "disabled");
    assertEquals("Click mining disabled", TweakIndicators.label(TweakIndicators.Tweak.CLICK_MINING));
    TweakIndicators.register(TweakIndicators.Tweak.CLICK_MINING, () -> null);
    assertNull(TweakIndicators.label(TweakIndicators.Tweak.CLICK_MINING));
  }
}
