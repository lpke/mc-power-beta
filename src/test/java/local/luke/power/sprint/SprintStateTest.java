package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SprintStateTest {
  @Test void multidirectionalSprintSupportsEveryDirectionAndActivationWithoutChangingForwardDefaults() {
    for (int activation = 0; activation <= 2; activation++) {
      for (float[] direction : directions()) {
        var s = settings(); s.activation = activation;
        var state = new SprintState();
        state.tick(s, true, direction[0], direction[1], true, true, false, true, true);
        assertEquals(direction[0] >= .8f, state.sprinting, "forward-only fallback");
        s.multidirectional = true;
        state.cancel(false);
        state.tick(s, true, direction[0], direction[1], true, true, false, true, true);
        assertTrue(state.sprinting, "activation " + activation);
        state.tick(s, true, direction[0], direction[1], false, true, false, true, true);
        assertEquals(activation != 1, state.sprinting, "Hold must release; Modern and Toggle persist");
      }
    }
  }

  @Test void multidirectionalTransitionsRetainSprintButStoppingAndDisablingClearIt() {
    for (int activation = 0; activation <= 2; activation++) {
      var s = settings(); s.activation = activation; s.multidirectional = true; s.doubleTap = false;
      var state = new SprintState();
      state.tick(s, true, 1, 0, true, true, false, true, true);
      for (float[] direction : directions()) {
        state.tick(s, true, direction[0], direction[1], activation == 1, true, false, true, true);
        assertTrue(state.sprinting);
      }
      state.tick(s, true, 0, 0, false, true, false, true, false);
      state.tick(s, true, -1, 0, false, true, false, true, true);
      assertFalse(state.sprinting, "stopping clears activation");
      state.tick(s, true, 0, 1, true, true, false, true, true);
      assertTrue(state.sprinting);
      s.multidirectional = false;
      state.tick(s, true, 0, 1, true, true, false, true, true);
      assertFalse(state.sprinting, "disabling restores forward-only behavior immediately");
      s.multidirectional = true;
      state.tick(s, true, -1, 0, false, true, false, true, true);
      assertFalse(state.sprinting);
    }
  }

  @Test void multidirectionalDoubleTapsBelongToOneDirectionAndStillRequireGroundContact() {
    for (int activation = 0; activation <= 2; activation++) {
      for (float[] direction : directions()) {
        var s = settings(); s.activation = activation; s.multidirectional = true;
        var state = new SprintState();
        state.tick(s, true, direction[0], direction[1], false, true, false, true, true);
        state.tick(s, true, 0, 0, false, true, false, true, false);
        state.tick(s, true, direction[0], direction[1], false, true, false, true, true);
        assertTrue(state.sprinting);
        state.tick(s, true, direction[0], direction[1], false, true, false, true, true);
        assertTrue(state.sprinting, "double tap persists in Hold mode too");
        state.cancel(false);
        state.tick(s, true, 0, 0, false, true, false, true, false);
        state.tick(s, true, direction[0], direction[1], false, false, false, true, true);
        state.tick(s, true, 0, 0, false, false, false, true, false);
        state.tick(s, true, direction[0], direction[1], false, false, false, true, true);
        assertFalse(state.sprinting);
      }
      var s = settings(); s.activation = activation; s.multidirectional = true;
      var state = new SprintState();
      for (float[] direction : directions()) {
        state.tick(s, true, direction[0], direction[1], false, true, false, true, true);
        assertFalse(state.sprinting, "changing direction is not a double tap");
      }
    }
  }

  @Test void sidewaysLandingRetainsEveryActivationAndAllNormalGates() {
    for (int activation = 0; activation <= 2; activation++) {
      var s = settings(); s.activation = activation; s.multidirectional = true;
      var state = new SprintState();
      state.land(s, true, 0, -1, true, false, true);
      state.tick(s, true, 0, -1, true, true, false, true, true);
      state.tick(s, true, 0, -1, false, true, false, true, true);
      assertTrue(state.sprinting);
      if (activation == 2) {
        state.tick(s, true, 0, -1, true, true, false, true, true);
        assertFalse(state.sprinting);
      }
      for (int blocked = 0; blocked < 4; blocked++) {
        s.stamina = s.noSprintInCombat = true;
        state = new SprintState();
        if (blocked == 2) state.combat(s);
        if (blocked == 3) { state.stamina = 0; state.exhausted = true; }
        state.land(s, blocked != 0, 0, -1, true, blocked == 1, true);
        assertFalse(state.sprinting);
        state.tick(s, blocked != 0, 0, -1, true, true, blocked == 1, true, true);
        assertFalse(state.sprinting);
      }
    }
  }

  @Test void multidirectionalSprintRetainsStaminaJumpCostsAndCreativeBypass() {
    var s = settings(); s.multidirectional = s.stamina = true; s.jumpCost = 4;
    var state = new SprintState(); state.stamina = 10;
    state.tick(s, true, 0, 1, true, true, false, true, true);
    assertEquals(9.5, state.stamina);
    state.jump(s);
    assertEquals(5.5, state.stamina);
    state.stamina = .5;
    state.tick(s, true, 0, 1, true, true, false, true, true);
    assertTrue(state.exhausted);
    assertFalse(state.sprinting);
    state.tick(s, true, 0, 1, true, true, false, false, true);
    assertTrue(state.sprinting);
    assertEquals(0, state.stamina);
    assertTrue(state.exhausted, "Creative bypass preserves Survival stamina state");
  }

  @Test void swimmingRemainsForwardOnlyAndDirectionalJumpImpulseIsNormalized() {
    var s = settings(); s.multidirectional = true;
    for (float[] direction : directions()) {
      var state = new SprintState();
      state.tick(s, true, SprintState.movementInput(direction[0], true), 0,
          true, true, false, true, true);
      assertEquals(direction[0] > 0, state.sprinting);
      for (float yaw : new float[] {0, 45, 90, 180, -90}) {
        double x = SprintState.jumpX(yaw, direction[0], direction[1]),
            z = SprintState.jumpZ(yaw, direction[0], direction[1]);
        assertEquals(.2, Math.hypot(x, z), 1e-8);
        double radians = yaw * (float) (Math.PI / 180);
        assertTrue(x * (direction[1] * Math.cos(radians) - direction[0] * Math.sin(radians))
            + z * (direction[0] * Math.cos(radians) + direction[1] * Math.sin(radians)) > 0);
      }
    }
    assertEquals(0, SprintState.jumpX(30, 0, 0));
    assertEquals(0, SprintState.jumpZ(30, 0, 0));
    assertEquals(.2, SprintState.jumpX(0, 0, 1), 1e-8);
    assertEquals(-.2, SprintState.jumpZ(0, -1, 0), 1e-8);
  }

  private static float[][] directions() {
    return new float[][] {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
  }

  @Test void landingRetainsEachActivationUntilMovementStopsWithoutRepeatingTheHeldPress() {
    for (int activation = 0; activation <= 2; activation++) {
      var settings = settings(); settings.activation = activation;
      var state = new SprintState(); state.land(settings,true,1,true,false,true);
      state.tick(settings, true, 1, true, false, false);
      assertTrue(state.sprinting);
      for (int i=0;i<20;i++) state.tick(settings,true,1,false,true,false);
      assertTrue(state.sprinting);
      if (activation == 2) { state.tick(settings,true,1,true,true,false); assertFalse(state.sprinting); }
      state.tick(settings,true,0,false,true,false);
      state.tick(settings,true,1,false,true,false);
      assertFalse(state.sprinting);
    }
  }

  @Test void landingCannotBypassPermissionCollisionCombatOrExhaustion() {
    var settings = settings(); settings.stamina = settings.noSprintInCombat = true;
    for (int blocked = 0; blocked < 4; blocked++) {
      var state = new SprintState();
      if (blocked == 2) state.combat(settings);
      if (blocked == 3) { state.stamina=0;state.exhausted=true; }
      state.land(settings,blocked!=0,1,false,blocked==1,true);
      assertFalse(state.sprinting,"handoff must validate before FOV publication");
      state.tick(settings,blocked!=0,1,false,true,blocked==1);
      assertFalse(state.sprinting);
      state.combatTicks=0;state.exhausted=false;state.stamina=100;
      state.tick(settings,true,1,false,true,false);
      assertFalse(state.sprinting,"blocked handoff must be consumed");
    }
  }
  private SprintSettings settings() {
    return new SprintSettings();
  }

  @Test
  void modernActivationRetainsSprintOnKeyReleaseAndStopsForBackwardsCollisionOrIneligibleInput() {
    var s = settings();
    var state = new SprintState();
    state.tick(s, true, 1, true, true, false);
    assertTrue(state.sprinting);
    state.tick(s, true, 1, false, true, false);
    assertTrue(state.sprinting);
    state.tick(s, true, .4f, false, true, false);
    assertTrue(state.sprinting);
    state.tick(s, true, 0, false, true, false);
    assertFalse(state.sprinting);
    state.tick(s, true, .79f, true, true, false);
    assertFalse(state.sprinting);
    state.tick(s, true, 1, true, true, false);
    assertTrue(state.sprinting);
    state.tick(s, true, 1, true, true, true);
    assertFalse(state.sprinting);
    state.tick(s, false, 1, true, true, false);
    assertFalse(state.sprinting);
  }

  @Test
  void doubleTapUsesNativeSevenTickWindowAndOnlyStartsOnGround() {
    var s = settings();
    var state = new SprintState();
    state.tick(s, true, 1, false, true, false);
    assertFalse(state.sprinting);
    state.tick(s, true, 0, false, true, false);
    state.tick(s, true, 1, false, true, false);
    assertTrue(state.sprinting);
    state.cancel(false);
    state.tick(s, true, 0, false, true, false);
    state.tick(s, true, 1, false, true, false);
    for (int i = 0; i < 8; i++) state.tick(s, true, 0, false, true, false);
    state.tick(s, true, 1, false, true, false);
    assertFalse(state.sprinting);
    state.tick(s, true, 0, false, false, false);
    state.tick(s, true, 1, false, false, false);
    assertFalse(state.sprinting);
  }

  @Test
  void doubleTapRetainsSprintWithEveryActivationAndExplicitToggleCanStopIt() {
    for (int activation = 0; activation <= 2; activation++) {
      var s = settings();
      s.activation = activation;
      var state = new SprintState();
      state.tick(s, true, 1, false, true, false);
      state.tick(s, true, 0, false, true, false);
      state.tick(s, true, 1, false, true, false);
      for (int n = 0; n < 20; n++) {
        state.tick(s, true, 1, false, true, false);
        assertTrue(state.sprinting, "activation " + activation);
      }
      if (activation == 2) {
        state.tick(s, true, 1, true, true, false);
        assertFalse(state.sprinting);
        state.tick(s, true, 1, false, true, false);
        assertFalse(state.sprinting);
      }
      state.tick(s, true, 0, false, true, false);
      state.tick(s, true, 1, false, true, false);
      assertFalse(state.sprinting);
    }
  }

  @Test
  void staminaExhaustionRechargeDelayCombatAndRestartThresholdUseElapsedTicks() {
    var s = settings();
    s.stamina = true;
    s.capacity = 10;
    s.drain = 10;
    s.recharge = s.rechargeStill = 2;
    s.rechargeDelay = 1;
    s.restartStamina = 50;
    var state = new SprintState();
    state.stamina = 10;
    for (int i = 0; i < 20; i++) state.tick(s, true, 1, true, true, false);
    assertEquals(0, state.stamina, 1e-9);
    assertTrue(state.exhausted);
    assertFalse(state.sprinting);
    state.combat(s);
    for (int i = 0; i < 40; i++) state.tick(s, true, 0, false, true, false);
    assertEquals(0, state.stamina, 1e-9);
    for (int i = 0; i < 160; i++) state.tick(s, true, 0, false, true, false);
    assertEquals(4, state.stamina, 1e-9);
    assertTrue(state.exhausted);
    for (int i = 0; i < 10; i++) state.tick(s, true, 0, false, true, false);
    assertEquals(5, state.stamina, 1e-9);
    assertFalse(state.exhausted);
    state.tick(s, true, 1, true, true, false);
    assertTrue(state.sprinting);
    s.noSprintInCombat = true;
    state.combat(s);
    state.tick(s, true, 1, true, true, false);
    assertFalse(state.sprinting);
  }

  @Test
  void modernGroundAndAirAccelerationAndJumpImpulseMatchReferenceConstants() {
    var s = settings();
    assertEquals(.13f, SprintState.acceleration(.1f, true, s), 1e-7);
    assertEquals(.025999999f, SprintState.acceleration(.02f, false, s), 1e-7);
    for (float yaw : new float[] {0, 45, 90, 180, -90, 720})
      assertEquals(.2, Math.hypot(SprintState.jumpX(yaw), SprintState.jumpZ(yaw)), 1e-8);
    assertEquals(-.2, SprintState.jumpX(90), 1e-7);
    assertEquals(.2, SprintState.jumpZ(0), 1e-7);
  }

  @Test
  void disabledWorldNeverSprintsAndEveryFovEffectStartsOff() {
    var s = new SprintSettings();
    s.validate();
    assertFalse(s.enabled || s.dynamicFov || s.creativeFov || s.spectatorFov || s.freecamFov);
    var state = new SprintState();
    state.tick(s, false, 1, true, true, false);
    assertFalse(state.sprinting);
    s.capacity = Double.NaN;
    assertThrows(IllegalArgumentException.class, s::validate);
  }

  @Test
  void worldEligibilityOwnsSprintRegardlessOfRetainedGlobalEnablement() {
    for (boolean oldEnabled : new boolean[]{false, true}) {
      var s = settings();
      s.enabled = oldEnabled;
      s.activation = 2;
      var state = new SprintState();
      state.tick(s, false, 1, true, true, false);
      assertFalse(state.sprinting);
      state.tick(s, true, 1, false, true, false);
      state.tick(s, true, 1, true, true, false);
      assertTrue(state.sprinting);
      state.tick(s, true, 1, false, true, false);
      assertTrue(state.sprinting);
      state.tick(s, false, 1, false, true, false);
      assertFalse(state.sprinting);
      state.tick(s, true, 1, false, true, false);
      assertFalse(state.sprinting, "Restoring permission must not restore a canceled toggle");
      assertEquals(oldEnabled, s.enabled);
    }
  }

  @Test
  void revocationClearsDoubleTapButPreservesStaminaAndCombatRecords() {
    var s = settings();
    var state = new SprintState();
    state.tick(s, true, 1, false, true, false);
    state.tick(s, true, 0, false, true, false);
    state.stamina = 35;
    state.rechargeTicks = 12;
    state.combat(s);
    state.cancel(false);
    assertEquals(35, state.stamina);
    assertEquals(12, state.rechargeTicks);
    assertEquals(160, state.combatTicks);
    state.tick(s, true, 1, false, true, false);
    assertFalse(state.sprinting);
  }
}
