package local.luke.power.input;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DebugInputTest {
  @BeforeEach void reset(){DebugInput.preflight(()->0);DebugInput.clear();DebugInput.context(true);}
  @AfterEach void clear(){DebugInput.preflight(()->0);DebugInput.clear();}
  @Test void plainOverlayWaitsForRelease(){DebugInput.press(true);assertTrue(DebugInput.managesOverlay());assertFalse(DebugInput.allows("key.power_controls.debug_hud",61));assertTrue(DebugInput.release());assertFalse(DebugInput.release());}
  @Test void chordDoesNotToggleOverlay(){DebugInput.press(true);DebugInput.claim(48);assertFalse(DebugInput.release());}
  @Test void releasingModifierFirstRetainsActionOwnership(){DebugInput.press(true);DebugInput.claim(46);DebugInput.release();assertFalse(DebugInput.allows("key.power_controls.zoom",46));assertFalse(DebugInput.allows("powerbeta.debug.location",46));DebugInput.released(46);assertTrue(DebugInput.allows("key.power_controls.zoom",46));}
  @Test void releasingActionFirstRetainsUsedModifier(){DebugInput.press(true);DebugInput.claim(16);DebugInput.released(16);assertFalse(DebugInput.release());}
  @Test void ordinaryHeldMovementSurvivesOtherDebugActions(){DebugInput.press(true,java.util.Set.of(17));DebugInput.claim(48);assertTrue(DebugInput.allows("key.forward",17));assertFalse(DebugInput.allows("powerbeta.waypoint",48));}
  @Test void freshOrdinaryPressesStayBlockedUntilDebugRelease(){DebugInput.press(true);assertFalse(DebugInput.allows("key.drop",16));assertTrue(DebugInput.allows("powerbeta.debug.help",16));DebugInput.claim(16);DebugInput.release();assertFalse(DebugInput.allows("key.drop",16));DebugInput.released(16);assertTrue(DebugInput.allows("key.drop",16));}
  @Test void heldMovementCannotRestartDuringDebugHold(){DebugInput.press(true,java.util.Set.of(17));DebugInput.released(17);assertFalse(DebugInput.allows("key.forward",17));}
  @Test void modePickerRetainsItsOwnGesture(){DebugInput.press(true);DebugInput.picker(62);assertFalse(DebugInput.suppressNative(62));assertTrue(DebugInput.allows("power_creative.picker",62));assertTrue(DebugInput.allows("power_creative.modifier",61));assertFalse(DebugInput.allows("powerbeta.infoHud",62));assertFalse(DebugInput.release());}
  @Test void disabledDebugDoesNotClaimOrdinaryKeys(){DebugInput.context(false);assertFalse(DebugInput.allows("powerbeta.debug.modifier",61));assertFalse(DebugInput.allows("powerbeta.debug.hitboxes",48));assertTrue(DebugInput.allows("powerbeta.waypoint",48));}
  @Test void losingContextCancelsPendingOverlay(){DebugInput.press(true);DebugInput.context(false);assertFalse(DebugInput.release());}
  @Test void separateModifierNeverChangesOverlay(){DebugInput.press(false);assertFalse(DebugInput.managesOverlay());assertFalse(DebugInput.release());}
  @Test void ordinaryPressHasNoDebugDelay(){assertTrue(DebugInput.allows("key.drop",16,true));assertFalse(DebugInput.claimed(16));}
  @Test void preflightBlocksQueuedActionInBothReleaseOrders(){
    for(boolean modifierFirst:new boolean[]{false,true}){
      reset();DebugInput.preflight(()->61);
      assertFalse(DebugInput.allows("power_building.hotbar.scroll",16,true));
      assertTrue(DebugInput.claimed(16));assertFalse(DebugInput.held());
      DebugInput.press(true);DebugInput.dispatched(61,true);DebugInput.claim(16);DebugInput.dispatched(16,true);
      DebugInput.preflight(()->0);
      if(modifierFirst){assertFalse(DebugInput.release());assertFalse(DebugInput.allows("key.drop",16,true));DebugInput.released(16);}
      else {DebugInput.released(16);assertFalse(DebugInput.release());}
      assertTrue(DebugInput.allows("power_building.hotbar.scroll",16,false));
    }
  }
  @Test void preflightPreservesOnlyPreviouslyDispatchedMovement(){
    DebugInput.dispatched(17,true);DebugInput.preflight(()->61);
    assertTrue(DebugInput.allows("key.forward",17,true));
    assertFalse(DebugInput.allows("key.drop",16,true));
    DebugInput.dispatched(16,true);DebugInput.press(true);
    assertTrue(DebugInput.allows("key.forward",17,true));
    assertFalse(DebugInput.allows("key.drop",16,true));
    DebugInput.released(17);assertFalse(DebugInput.allows("key.forward",17,true));
  }
  @Test void preflightDoesNotClaimIdleKeysOrTheModifierItself(){
    DebugInput.preflight(()->61);
    assertFalse(DebugInput.allows("key.drop",16,false));assertFalse(DebugInput.claimed(16));
    assertFalse(DebugInput.allows("key.power_controls.debug_hud",61,true));assertFalse(DebugInput.claimed(61));
    DebugInput.press(true);assertTrue(DebugInput.release());
  }
  @Test void preflightCannotDisableModePicker(){
    DebugInput.preflight(()->61);
    assertTrue(DebugInput.allows("power_creative.modifier",61,true));
    assertTrue(DebugInput.allows("power_creative.picker",62,true));
  }
  @Test void canceledPreflightCannotReuseAnOldHeldSnapshot(){
    DebugInput.dispatched(17,true);DebugInput.preflight(()->61);DebugInput.allows("key.drop",16,true);
    DebugInput.preflight(()->0);DebugInput.allows("key.left",30,false);
    DebugInput.dispatched(17,false);DebugInput.dispatched(30,true);DebugInput.preflight(()->61);
    assertTrue(DebugInput.allows("key.left",30,true));assertFalse(DebugInput.allows("key.forward",17,true));
  }
  @Test void firstBatchMouseDebugActionOwnsItsPressAndConsumesPlainF3(){
    DebugInput.preflight(()->61);
    assertFalse(DebugInput.allows("native",-99,true));
    assertTrue(DebugInput.actionHeld());assertTrue(DebugInput.allows("powerbeta.debug.hitboxes",-99,true));
    DebugInput.claim(-99);DebugInput.dispatched(-99,true);
    DebugInput.press(true);assertTrue(DebugInput.suppressNative(-99));
    DebugInput.preflight(()->0);assertFalse(DebugInput.release());
    assertTrue(DebugInput.suppressNative(-99));DebugInput.released(-99);assertFalse(DebugInput.suppressNative(-99));
  }
  @Test void physicalPrepareAndModifierEventShareOneEntryGeneration(){
    long before=DebugInput.entryGeneration();DebugInput.preflight(()->61);
    DebugInput.allows("key.drop",16,true);assertEquals(before+1,DebugInput.entryGeneration());
    DebugInput.allows("key.forward",17,true);DebugInput.press(true);assertEquals(before+1,DebugInput.entryGeneration());
  }
  @Test void completeQueuedHoldRemainsObservableAfterReleaseAndContextClear(){
    long before=DebugInput.entryGeneration();DebugInput.press(true);DebugInput.release();DebugInput.clear();
    assertEquals(before+1,DebugInput.entryGeneration());DebugInput.context(false);assertEquals(before+1,DebugInput.entryGeneration());
  }
  @Test void ordinaryInputNeverAdvancesTheDebugEntryBoundary(){
    long before=DebugInput.entryGeneration();DebugInput.dispatched(17,true);DebugInput.allows("key.forward",17,true);
    assertEquals(before,DebugInput.entryGeneration());
  }
}
