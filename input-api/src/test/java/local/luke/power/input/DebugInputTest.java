package local.luke.power.input;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DebugInputTest {
  @BeforeEach void reset(){DebugInput.clear();DebugInput.context(true);}
  @Test void plainOverlayWaitsForRelease(){DebugInput.press(true);assertTrue(DebugInput.managesOverlay());assertFalse(DebugInput.allows("key.power_controls.debug_hud",61));assertTrue(DebugInput.release());assertFalse(DebugInput.release());}
  @Test void chordDoesNotToggleOverlay(){DebugInput.press(true);DebugInput.claim(48);assertFalse(DebugInput.release());}
  @Test void releasingModifierFirstRetainsActionOwnership(){DebugInput.press(true);DebugInput.claim(46);DebugInput.release();assertFalse(DebugInput.allows("key.power_controls.zoom",46));assertFalse(DebugInput.allows("powerbeta.debug.location",46));DebugInput.released(46);assertTrue(DebugInput.allows("key.power_controls.zoom",46));}
  @Test void releasingActionFirstRetainsUsedModifier(){DebugInput.press(true);DebugInput.claim(16);DebugInput.released(16);assertFalse(DebugInput.release());}
  @Test void ordinaryHeldMovementSurvivesOtherDebugActions(){DebugInput.press(true);DebugInput.claim(48);assertTrue(DebugInput.allows("key.forward",17));assertFalse(DebugInput.allows("powerbeta.waypoint",48));}
  @Test void modePickerRetainsItsOwnGesture(){DebugInput.press(true);DebugInput.picker(62);assertFalse(DebugInput.suppressNative(62));assertTrue(DebugInput.allows("power_creative.picker",62));assertTrue(DebugInput.allows("power_creative.modifier",61));assertFalse(DebugInput.allows("powerbeta.infoHud",62));assertFalse(DebugInput.release());}
  @Test void disabledDebugDoesNotClaimOrdinaryKeys(){DebugInput.context(false);assertFalse(DebugInput.allows("powerbeta.debug.modifier",61));assertFalse(DebugInput.allows("powerbeta.debug.hitboxes",48));assertTrue(DebugInput.allows("powerbeta.waypoint",48));}
  @Test void losingContextCancelsPendingOverlay(){DebugInput.press(true);DebugInput.context(false);assertFalse(DebugInput.release());}
  @Test void separateModifierNeverChangesOverlay(){DebugInput.press(false);assertFalse(DebugInput.managesOverlay());assertFalse(DebugInput.release());}
}
