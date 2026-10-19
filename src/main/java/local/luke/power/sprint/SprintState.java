package local.luke.power.sprint;

/** Twenty-Hz sprint and stamina state, independently testable without native input. */
public final class SprintState {
  public boolean sprinting, exhausted;
  public double stamina = 100;
  public int combatTicks, rechargeTicks;
  private boolean keyWasDown, forwardWasDown, latched, tapped;
  private int doubleTap;

  public void cancel(boolean keyDown) {
    sprinting = false;
    latched = false;
    tapped = false;
    keyWasDown = keyDown;
    forwardWasDown = true;
    doubleTap = 0;
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
    stamina = Math.max(0, Math.min(s.capacity, stamina));
    if (doubleTap > 0) doubleTap--;
    boolean pressed = sprintKey && !keyWasDown,
        forwardDown = forward >= .8f,
        forwardPress = forwardDown && !forwardWasDown;
    boolean tap = false;
    if (eligible && s.doubleTap && onGround && forwardPress) {
      tap = doubleTap > 0;
      doubleTap = s.doubleTapTicks;
    }
    if (tap) tapped = true;
    if (s.activation == 2 && pressed) {
      latched = !(latched || tapped);
      tapped = false;
    }
    boolean can =
        eligible
            && s.enabled
            && (!s.noSprintInCombat || combatTicks == 0)
            && (!s.stamina || !exhausted && stamina > 0);
    if (!can || forward <= 0 || collision) {
      sprinting = false;
      latched = false;
      tapped = false;
    } else if (s.activation == 1) sprinting = forwardDown && (sprintKey || tapped);
    else if (s.activation == 2) sprinting = forwardDown && (latched || tapped);
    else if (forwardDown && (sprintKey || tap)) sprinting = true;
    if (!eligible) {
      sprinting = false;
      latched = false;
      tapped = false;
      doubleTap = 0;
    }
    if (s.stamina) {
      if (sprinting) {
        stamina = Math.max(0, stamina - s.drain / 20);
        rechargeTicks = (int) Math.ceil(s.rechargeDelay * 20);
      } else if (rechargeTicks > 0) rechargeTicks--;
      else if (combatTicks == 0 || s.rechargeInCombat)
        stamina = Math.min(s.capacity, stamina + s.recharge / 20);
      if (stamina <= 0) {
        exhausted = true;
        sprinting = false;
        latched = false;
        tapped = false;
      }
      if (exhausted && stamina + 1e-9 >= s.capacity * s.restartStamina / 100 && stamina > 0)
        exhausted = false;
    } else exhausted = false;
    keyWasDown = sprintKey;
    forwardWasDown = forwardDown;
    if (combatTicks > 0) combatTicks--;
  }

  public void jump(SprintSettings s) {
    if (s.stamina && sprinting) {
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
    return -Math.sin(yaw * (float) (Math.PI / 180)) * .2;
  }

  public static double jumpZ(float yaw) {
    return Math.cos(yaw * (float) (Math.PI / 180)) * .2;
  }
}
