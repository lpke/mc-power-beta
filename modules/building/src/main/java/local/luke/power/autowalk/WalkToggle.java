package local.luke.power.autowalk;

/** Pure input state, independent of Minecraft and LWJGL. */
public final class WalkToggle {
  private boolean active;
  private boolean keyWasDown, holdEligible, longHold;
  private long pressedAt;
  private int direction = 1;

  public boolean update(boolean keyDown, boolean canWalk) {
    return update(keyDown, canWalk, false, 0, 350);
  }

  public boolean update(boolean keyDown, boolean canWalk, boolean holdToWalk, long now, int holdMillis) {
    return update(keyDown, canWalk, holdToWalk, now, holdMillis, false);
  }

  public boolean update(boolean keyDown, boolean canWalk, boolean holdToWalk, long now, int holdMillis,
                        boolean walkingBackwards) {
    boolean before = active;
    if (!canWalk) {
      stop();
    } else if (keyDown && !keyWasDown) {
      active = !active;
      direction = walkingBackwards ? -1 : 1;
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

  /** Native key-down owns activation; polling only maintains a held gesture. */
  public void press(boolean canWalk, boolean holdToWalk, long now, boolean walkingBackwards) {
    if (!canWalk) { cancel(); return; }
    if (keyWasDown) return;
    active = !active;
    direction = walkingBackwards ? -1 : 1;
    pressedAt = now; holdEligible = holdToWalk; longHold = false; keyWasDown = true;
  }

  public void poll(boolean keyDown, boolean canWalk, boolean holdToWalk, long now, int holdMillis) {
    if (!canWalk) { cancel(); }
    if (canWalk && holdEligible && holdToWalk && now - pressedAt >= Math.max(100, holdMillis) * 1_000_000L) {
      if (keyDown && !longHold) { active = true; longHold = true; }
      if (!keyDown) active = false;
    }
    if (!keyDown) { keyWasDown = false; holdEligible = false; longHold = false; }
  }

  public void cancel() { stop(); keyWasDown = true; }

  public boolean isActive() {
    return active;
  }

  public void unfocused() {
    holdEligible = false;
    longHold = false;
    keyWasDown = true;
  }

  public float input() { return active ? direction : 0; }

  public float movementInput(float vanillaInput, boolean forwardHeld, boolean backHeld, boolean sneaking) {
    boolean directionHeld = direction < 0 ? backHeld : forwardHeld;
    return directionHeld ? vanillaInput : vanillaInput + direction * (sneaking ? 0.3F : 1.0F);
  }

  public void stop() {
    holdEligible = false; longHold = false;
    active = false;
  }

  public static float forwardInput(float vanillaInput, boolean forwardHeld, boolean sneaking) {
    return forwardHeld ? vanillaInput : vanillaInput + (sneaking ? 0.3F : 1.0F);
  }
}
