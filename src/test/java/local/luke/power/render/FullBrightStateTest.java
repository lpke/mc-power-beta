package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FullBrightStateTest {
  @Test void spectatorOverridesEndAtExitAndPreserveEarlierManualState() {
    for (boolean manual : new boolean[] {false, true}) {
      Object world = new Object(); var state = new FullBrightState();
      state.update(world, true, false, true);
      if (manual) state.toggle();
      state.update(world, true, true, true); assertTrue(state.active());
      state.toggle(); assertFalse(state.active());
      state.update(world, true, true, true); assertFalse(state.active());
      state.update(world, true, false, true); assertEquals(manual, state.active());
      state.update(world, true, true, true); assertTrue(state.active());
      state.update(world, false, false, true); assertEquals(manual, state.active());
      state.update(world, true, false, true); assertEquals(manual, state.active());
    }
  }
  @Test void manualBrightnessWorksWithoutCheatsAndClearsOnWorldExit() {
    Object world = new Object(); var state = new FullBrightState();
    state.update(world, false, false, true); state.toggle(); assertTrue(state.active());
    state.update(world, false, false, true); assertTrue(state.active());
    state.update(world, true, true, true); state.toggle(); assertFalse(state.active());
    state.update(world, false, true, true); assertTrue(state.active());
    state.update(null, false, false, false); assertFalse(state.active());
  }
  @Test void preferenceChangesAndWorldChangesClearVisitOverrides() {
    Object world = new Object(); var state = new FullBrightState();
    state.update(world, true, true, true); state.toggle(); assertFalse(state.active());
    state.update(world, true, true, false); state.toggle(); assertTrue(state.active());
    state.update(world, true, false, false); assertTrue(state.active());
    state.update(new Object(), true, false, false); assertFalse(state.active());
    state.update(world, true, true, true); assertTrue(state.active());
    state.update(world, true, true, false); assertFalse(state.active());
  }
}
