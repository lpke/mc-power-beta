package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SprintPolishTest {
  private SprintSettings settings() {
    var settings = new SprintSettings();
    settings.stamina = true; settings.activation = 1; settings.doubleTap = false;
    settings.capacity = 10; settings.drain = 10; settings.recharge = 4;
    settings.rechargeDelay = 1; settings.restartStamina = 50; settings.jumpCost = 4;
    return settings;
  }

  @Test void zeroStaminaAndExhaustionDoNotBlockOrChangeDuringAStaminaFreeVisit() {
    var settings = settings(); var state = new SprintState();
    state.stamina = 0; state.exhausted = true; state.rechargeTicks = 17; state.combatTicks = 8;
    for (int tick = 0; tick < 80; tick++) {
      state.tick(settings, true, 1, true, true, false, false);
      assertTrue(state.sprinting, "stamina-free ground sprint must remain available");
      state.jump(settings, false);
      assertEquals(0, state.stamina); assertTrue(state.exhausted); assertEquals(17, state.rechargeTicks);
    }
    assertEquals(0, state.combatTicks, "combat cooldown still measures elapsed game ticks");
    state.tick(settings, true, 1, true, true, false, true);
    assertFalse(state.sprinting); assertEquals(0, state.stamina); assertTrue(state.exhausted);
    assertEquals(16, state.rechargeTicks, "Survival resumes its existing delay");
  }

  @Test void inactiveFlightVisitsNeitherRechargeNorClampTheSavedSurvivalCapacity() {
    var settings = settings(); var state = new SprintState();
    state.stamina = 80; state.exhausted = true; state.rechargeTicks = 9;
    for (int tick = 0; tick < 40; tick++) state.tick(settings, false, 0, false, false, false, false);
    assertEquals(80, state.stamina); assertTrue(state.exhausted); assertEquals(9, state.rechargeTicks);
    state.tick(settings, true, 0, false, true, false, true);
    assertEquals(10, state.stamina, "only Survival applies the selected capacity");
    assertFalse(state.exhausted); assertEquals(8, state.rechargeTicks);
  }

  @Test void staminaFreeMovementAndJumpKeepTheExactReserveUntilSurvivalDrainResumes() {
    var settings = settings(); var state = new SprintState();
    state.stamina = 7.5; state.rechargeTicks = 12;
    for (int tick = 0; tick < 20; tick++) {
      state.tick(settings, true, 1, true, true, false, false); state.jump(settings, false);
    }
    assertTrue(state.sprinting); assertEquals(7.5, state.stamina); assertEquals(12, state.rechargeTicks);
    state.tick(settings, true, 1, true, true, false, true);
    assertTrue(state.sprinting); assertEquals(7, state.stamina); assertEquals(20, state.rechargeTicks);
    state.jump(settings, true); assertEquals(3, state.stamina);
    state.jump(settings, true); assertEquals(0, state.stamina); assertTrue(state.exhausted); assertFalse(state.sprinting);
  }

  @Test void returningToSurvivalRetainsExhaustionUntilItsDelayAndRestartThresholdComplete() {
    var settings = settings(); var state = new SprintState();
    state.stamina = 2; state.exhausted = true; state.rechargeTicks = 3;
    for (int tick = 0; tick < 60; tick++) state.tick(settings, true, 1, true, true, false, false);
    assertEquals(2, state.stamina); assertTrue(state.exhausted); assertEquals(3, state.rechargeTicks);
    for (int tick = 0; tick < 3; tick++) {
      state.tick(settings, true, 1, true, true, false, true);
      assertFalse(state.sprinting); assertEquals(2, state.stamina); assertTrue(state.exhausted);
    }
    for (int tick = 0; tick < 14; tick++) state.tick(settings, true, 1, true, true, false, true);
    assertEquals(4.8, state.stamina, 1e-9); assertTrue(state.exhausted); assertFalse(state.sprinting);
    state.tick(settings, true, 1, true, true, false, true);
    assertEquals(5, state.stamina, 1e-9); assertFalse(state.exhausted); assertFalse(state.sprinting);
    state.tick(settings, true, 1, true, true, false, true);
    assertTrue(state.sprinting); assertEquals(4.5, state.stamina, 1e-9);
  }

  @Test void onlyTheOwningSurvivalPreferenceCanClearDisabledStaminaExhaustion() {
    var settings = settings(); settings.stamina = false;
    var state = new SprintState(); state.stamina = 2; state.exhausted = true; state.rechargeTicks = 7;
    state.tick(settings, true, 1, true, true, false, false);
    assertTrue(state.exhausted); assertEquals(2, state.stamina); assertEquals(7, state.rechargeTicks);
    state.tick(settings, true, 1, true, true, false, true);
    assertFalse(state.exhausted); assertTrue(state.sprinting); assertEquals(2, state.stamina);
  }

  @Test void staminaIconsUseBoundedHalfIconUnitsForFiniteReserves() {
    assertEquals(0, StaminaIcons.units(0, 100));
    assertEquals(1, StaminaIcons.units(.001, 100));
    assertEquals(1, StaminaIcons.units(5, 100));
    assertEquals(2, StaminaIcons.units(5.001, 100));
    assertEquals(10, StaminaIcons.units(50, 100));
    assertEquals(20, StaminaIcons.units(100, 100));
    assertEquals(20, StaminaIcons.units(250, 100));
    assertEquals(0, StaminaIcons.units(-1, 100));
    assertEquals(0, StaminaIcons.units(Double.NaN, 100));
    assertEquals(0, StaminaIcons.units(50, Double.POSITIVE_INFINITY));
    assertEquals(0, StaminaIcons.units(50, 0));
    for (double capacity : new double[]{1, 10, 1000}) for (int unit = 0; unit <= 20; unit++)
      assertEquals(unit, StaminaIcons.units(capacity * unit / 20, capacity), "capacity=" + capacity + " unit=" + unit);
  }

  @Test void armorCollisionUsesActualHorizontalOverlapAndLeavesTenPixelsBetweenRows() {
    assertEquals(138, StaminaIcons.y(251, 148, true, 170, 148, 180));
    assertEquals(148, StaminaIcons.y(251, 148, false, 170, 148, 180));
    assertEquals(148, StaminaIcons.y(251, 148, true, 251, 148, 180), "touching right edges do not overlap");
    assertEquals(148, StaminaIcons.y(251, 148, true, 89, 148, 180), "touching left edges do not overlap");
    assertEquals(138, StaminaIcons.y(251, 138, true, 170, 148, 180));
    assertEquals(10, StaminaIcons.y(251, -32, true, 170, 0, 180), "top armor moves stamina below it");
    assertEquals(15, StaminaIcons.y(251, -31, true, 170, 5, 180));
  }

  @Test void screenClampingCannotPutStaminaBackOnTopOfArmorAtEitherEdge() {
    for (int height : new int[]{48, 180, 360}) for (int armorY : new int[]{0, height - 9})
      for (int baseline : new int[]{-50, -32, 0, height - 9, height + 20}) {
        int y = StaminaIcons.y(251, baseline, true, 170, armorY, height);
        assertTrue(y >= 0 && y + 9 <= height, "icon row leaves screen");
        assertTrue(y + 9 <= armorY || y >= armorY + 9,
            "screen clamp overlaps armor: height=" + height + " armor=" + armorY + " baseline=" + baseline + " y=" + y);
      }
    assertEquals(0, StaminaIcons.y(251, -100, false, 170, 0, 180));
    assertEquals(171, StaminaIcons.y(251, 500, false, 170, 0, 180));
    assertEquals(0, StaminaIcons.y(251, 0, false, 170, 0, 0));
  }
}
