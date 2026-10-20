package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SprintStateTest {
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
