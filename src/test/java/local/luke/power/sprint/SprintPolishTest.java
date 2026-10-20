package local.luke.power.sprint;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SprintPolishTest {
  private SprintSettings settings() {
    var settings = new SprintSettings();
    settings.stamina = true; settings.activation = 1; settings.doubleTap = false;
    settings.capacity = 10; settings.drain = 10; settings.recharge = settings.rechargeStill = 4;
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

  @Test void ninePixelScreenClampingCannotPutStaminaBackOnTopOfArmorAtEitherEdge() {
    for (int height : new int[]{48, 180, 360}) for (int armorY : new int[]{0, height - 9})
      for (int baseline : new int[]{-50, -32, 0, height - 9, height + 20}) {
        int y = StaminaIcons.y(251, baseline, true, 170, armorY, height);
        assertTrue(y >= 0 && y + 9 <= height, "native nine-pixel icon row leaves screen");
        assertTrue(y + 9 <= armorY || y >= armorY + 9,
            "screen clamp overlaps armor: height=" + height + " armor=" + armorY + " baseline=" + baseline + " y=" + y);
      }
    assertEquals(0, StaminaIcons.y(251, -100, false, 170, 0, 180));
    assertEquals(171, StaminaIcons.y(251, 500, false, 170, 0, 180));
    assertEquals(0, StaminaIcons.y(251, 0, false, 170, 0, 0));
  }

  @Test void lightningBorderFitsExactlyInsideNinePixelsAndOnlyNonzeroThresholdsMarkAnIcon() {
    assertTrue(StaminaIcons.border(5, 0)); assertTrue(StaminaIcons.border(2, 8));
    for (int p = -1; p <= 9; p++) {
      assertFalse(StaminaIcons.border(p, -1)); assertFalse(StaminaIcons.border(p, 9));
      assertFalse(StaminaIcons.border(-1, p)); assertFalse(StaminaIcons.border(9, p));
    }
    assertEquals(-1, StaminaIcons.restartIcon(0));
    assertEquals(-1, StaminaIcons.restartIcon(Double.NaN));
    assertEquals(0, StaminaIcons.restartIcon(.1)); assertEquals(1, StaminaIcons.restartIcon(20));
    assertEquals(9, StaminaIcons.restartIcon(100));
  }

  @Test void lightningDrainsFromTheLeftAndRetainsTheRightmostReserveWithNativeSpacing() {
    assertEquals(72, StaminaIcons.x(0, 0)); assertEquals(0, StaminaIcons.x(0, 9));
    for (int icon = 1; icon < 10; icon++) assertEquals(8, StaminaIcons.x(17, icon - 1) - StaminaIcons.x(17, icon));
    assertEquals(0xe2b32f, StaminaIcons.color(0, 2, 0, 3, 4));
    assertEquals(0x51462e, StaminaIcons.color(1, 2, 0, 3, 4));
    assertEquals(0xe2b32f, StaminaIcons.color(0, 1, 0, 3, 4));
    assertEquals(0x51462e, StaminaIcons.color(0, 1, 0, 6, 4));
    assertEquals(0xe2b32f, StaminaIcons.color(9, 20, 0, 3, 4));
    assertEquals(0x51462e, StaminaIcons.color(9, 18, 0, 3, 4));
  }

  @Test void restartRegionDarkensOnlyDepletedReserveAndNeverChangesFilledColorsOrBorder() {
    for (int icon = 0; icon < 10; icon++) {
      assertEquals(icon < 4 ? 0x282317 : 0x51462e, StaminaIcons.color(icon, 0, 40, 3, 4));
      assertEquals(0xe2b32f, StaminaIcons.color(icon, 20, 40, 3, 4));
      assertEquals(0, StaminaIcons.color(icon, 0, 40, 5, 0));
      assertEquals(-1, StaminaIcons.color(icon, 20, 40, 0, 0));
    }
    assertEquals(0xffeaa0, StaminaIcons.color(0, 20, 50, 4, 1));
    assertEquals(0xbf8c22, StaminaIcons.color(0, 20, 50, 7, 4));
    assertEquals(0x51462e, StaminaIcons.color(0, 0, 0, 3, 4));
    assertEquals(0x282317, StaminaIcons.color(9, 0, 100, 3, 4));
  }

  @Test void stationaryAndMovingRecoveryUseIndependentRatesWithSharedDelayCombatAndOwnership() {
    var settings = settings(); settings.rechargeDelay = 0;
    settings.capacity = 100; settings.recharge = 4; settings.rechargeStill = 6;
    var moving = new SprintState(); var still = new SprintState();
    moving.stamina = still.stamina = 0; moving.exhausted = still.exhausted = true;
    for (int tick = 0; tick < 20; tick++) {
      moving.tick(settings, true, 0, false, true, false, true, true);
      still.tick(settings, true, 0, false, true, false, true, false);
    }
    assertEquals(4, moving.stamina, 1e-9); assertEquals(6, still.stamina, 1e-9);
    moving.combatTicks = still.combatTicks = 2;
    moving.tick(settings, true, 0, false, true, false, true, true);
    still.tick(settings, true, 0, false, true, false, true, false);
    assertEquals(4, moving.stamina, 1e-9); assertEquals(6, still.stamina, 1e-9);
    still.tick(settings, true, 0, false, true, false, false, false);
    assertEquals(6, still.stamina, 1e-9, "flight/Creative never recover the saved reserve");
    settings.rechargeStill = Double.NaN; assertThrows(IllegalArgumentException.class, settings::validate);
  }
}
