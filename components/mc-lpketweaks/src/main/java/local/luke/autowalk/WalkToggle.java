package local.luke.autowalk;

/** Pure input state, independent of Minecraft and LWJGL. */
public final class WalkToggle {
  private boolean active;
  private boolean keyWasDown;

  public boolean update(boolean keyDown, boolean canWalk) {
    boolean before = active;
    if (!canWalk) {
      active = false;
    } else if (keyDown && !keyWasDown) {
      active = !active;
    }
    keyWasDown = keyDown;
    return before != active;
  }

  public boolean isActive() {
    return active;
  }

  public void stop() {
    active = false;
  }

  public static float forwardInput(float vanillaInput, boolean forwardHeld, boolean sneaking) {
    return forwardHeld ? vanillaInput : vanillaInput + (sneaking ? 0.3F : 1.0F);
  }
}
