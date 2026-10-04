package local.luke.power.autowalk;

/** Pure input state, independent of Minecraft and LWJGL. */
public final class WalkToggle {
  private boolean active;
  private boolean keyWasDown, holdEligible, longHold;
  private long pressedAt;

  public boolean update(boolean keyDown, boolean canWalk) {
    return update(keyDown, canWalk, false, 0, 350);
  }

  public boolean update(boolean keyDown, boolean canWalk, boolean holdToWalk, long now, int holdMillis) {
    boolean before = active;
    if (!canWalk) {
      stop();
    } else if (keyDown && !keyWasDown) {
      active = !active;
      pressedAt = now; holdEligible = holdToWalk; longHold = false;
    }
    if (canWalk && holdEligible && holdToWalk && now - pressedAt >= Math.max(100, holdMillis) * 1_000_000L) {
      if (keyDown && !longHold) { active = true; longHold = true; }
      if (!keyDown) active = false;
    }
    if (!keyDown) { holdEligible = false; longHold = false; }
    keyWasDown = keyDown;
    return before != active;
  }

  public boolean isActive() {
    return active;
  }

  public void stop() {
    holdEligible = false; longHold = false;
    active = false;
  }

  public static float forwardInput(float vanillaInput, boolean forwardHeld, boolean sneaking) {
    return forwardHeld ? vanillaInput : vanillaInput + (sneaking ? 0.3F : 1.0F);
  }
}
