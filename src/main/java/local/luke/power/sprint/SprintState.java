package local.luke.power.sprint;

/** Twenty-Hz sprint and stamina state, independently testable without native input. */
public final class SprintState {
  public boolean sprinting, exhausted;
  public double stamina = 100;
  public int combatTicks, rechargeTicks;
  private boolean keyWasDown, latched, tapped;
  private int directionWasDown, doubleTap, doubleTapDirection;

  /** Continue an already-active flight gesture; normal tick validation still owns permission/stamina. */
  public void land(SprintSettings settings, boolean eligible, float forward, boolean sprintKey,
      boolean collision, boolean usesStamina) {
    land(settings, eligible, forward, 0, sprintKey, collision, usesStamina);
  }

  public void land(SprintSettings settings, boolean eligible, float forward, float sideways,
      boolean sprintKey, boolean collision, boolean usesStamina) {
    cancel(sprintKey);
    sprinting = tapped = canSprint(settings, eligible, usesStamina)
        && direction(settings, forward, sideways) != 0 && !collision;
  }

  private boolean canSprint(SprintSettings settings, boolean eligible, boolean usesStamina) {
    return eligible && (!settings.noSprintInCombat || combatTicks == 0)
        && (!settings.stamina || !usesStamina || !exhausted && stamina > 0);
  }

  public void cancel(boolean keyDown) {
    sprinting = false;
    latched = false;
    tapped = false;
    keyWasDown = keyDown;
    directionWasDown = 5; // Suppress a held direction until its next release/change.
    doubleTap = 0;
    doubleTapDirection = 0;
  }

  public void combat(SprintSettings settings) {
    combatTicks = (int) Math.ceil(settings.combatTime * 20);
  }

  public void tick(
      SprintSettings s,
      boolean eligible,
      float forward,
      boolean sprintKey,
      boolean onGround,
      boolean collision) {
    tick(s, eligible, forward, sprintKey, onGround, collision, true);
  }

  public void tick(SprintSettings s, boolean eligible, float forward, boolean sprintKey,
      boolean onGround, boolean collision, boolean usesStamina) {
    tick(s, eligible, forward, sprintKey, onGround, collision, usesStamina, forward != 0);
  }

  public void tick(SprintSettings s, boolean eligible, float forward, boolean sprintKey,
      boolean onGround, boolean collision, boolean usesStamina, boolean moving) {
    tick(s, eligible, forward, 0, sprintKey, onGround, collision, usesStamina, moving);
  }

  public void tick(SprintSettings s, boolean eligible, float forward, float sideways,
      boolean sprintKey, boolean onGround, boolean collision, boolean usesStamina, boolean moving) {
    boolean staminaEnabled = s.stamina && usesStamina;
    if (usesStamina) stamina = Math.max(0, Math.min(s.capacity, stamina));
    if (doubleTap > 0) doubleTap--;
    int direction = direction(s, forward, sideways);
    boolean pressed = sprintKey && !keyWasDown,
        directionDown = direction != 0,
        directionPress = directionDown && directionWasDown != direction && directionWasDown != 5;
    boolean tap = false;
    if (eligible && s.doubleTap && onGround && directionPress) {
      tap = doubleTap > 0 && doubleTapDirection == direction;
      doubleTap = s.doubleTapTicks;
      doubleTapDirection = direction;
    }
    if (tap) tapped = true;
    if (s.activation == 2 && pressed) {
      latched = !(latched || tapped);
      tapped = false;
    }
    boolean can = canSprint(s, eligible, usesStamina);
    if (!can || (s.multidirectional ? forward == 0 && sideways == 0 : forward <= 0) || collision) {
      sprinting = false;
      latched = false;
      tapped = false;
    } else if (s.activation == 1) sprinting = directionDown && (sprintKey || tapped);
    else if (s.activation == 2) sprinting = directionDown && (latched || tapped);
    else if (directionDown && (sprintKey || tap)) sprinting = true;
    if (!eligible) {
      sprinting = false;
      latched = false;
      tapped = false;
      doubleTap = 0;
      doubleTapDirection = 0;
    }
    if (staminaEnabled) {
      if (sprinting) {
        stamina = Math.max(0, stamina - s.drain / 20);
        rechargeTicks = (int) Math.ceil(s.rechargeDelay * 20);
      } else if (rechargeTicks > 0) rechargeTicks--;
      else if (combatTicks == 0 || s.rechargeInCombat)
        stamina = Math.min(s.capacity, stamina + (moving ? s.recharge : s.rechargeStill) / 20);
      if (stamina <= 0) {
        exhausted = true;
        sprinting = false;
        latched = false;
        tapped = false;
      }
      if (exhausted && stamina + 1e-9 >= s.capacity * s.restartStamina / 100 && stamina > 0)
        exhausted = false;
    } else if (usesStamina) exhausted = false;
    keyWasDown = sprintKey;
    directionWasDown = direction;
    if (combatTicks > 0) combatTicks--;
  }

  private static int direction(SprintSettings settings, float forward, float sideways) {
    if (!settings.multidirectional) return forward >= .8f ? 1 : 0;
    if (forward * forward + sideways * sideways < .8f * .8f) return 0;
    return 3 * (int) Math.signum(forward) + (int) Math.signum(sideways);
  }

  /** Swimming keeps its forward-only sprint threshold independently of on-foot preferences. */
  public static float movementInput(float forward, boolean swimming) {
    return swimming ? forward > 0 ? 1 : 0 : forward;
  }

  public void jump(SprintSettings s) {
    jump(s, true);
  }

  public void jump(SprintSettings s, boolean usesStamina) {
    if (usesStamina && s.stamina && sprinting) {
      stamina = Math.max(0, stamina - s.jumpCost);
      if (stamina == 0) {
        sprinting = false;
        exhausted = true;
      }
    }
  }

  public static float acceleration(float nativeValue, boolean ground, SprintSettings settings) {
    return ground
        ? (float) (nativeValue * settings.speed / 100)
        : (float) (.02 * settings.speed / 100);
  }

  public static double jumpX(float yaw) {
    return jumpX(yaw, 1, 0);
  }

  public static double jumpX(float yaw, float forward, float sideways) {
    double length = Math.hypot(forward, sideways);
    if (length == 0) return 0;
    double angle = yaw * (float) (Math.PI / 180);
    return (sideways * Math.cos(angle) - forward * Math.sin(angle)) * .2 / length;
  }

  public static double jumpZ(float yaw) {
    return jumpZ(yaw, 1, 0);
  }

  public static double jumpZ(float yaw, float forward, float sideways) {
    double length = Math.hypot(forward, sideways);
    if (length == 0) return 0;
    double angle = yaw * (float) (Math.PI / 180);
    return (forward * Math.cos(angle) + sideways * Math.sin(angle)) * .2 / length;
  }
}
