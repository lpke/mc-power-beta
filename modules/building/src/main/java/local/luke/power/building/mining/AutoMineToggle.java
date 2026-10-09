package local.luke.power.building.mining;

/** A cancelled action requires a fresh press, including after focus/menu changes. */
public final class AutoMineToggle {
  private boolean active, wasDown;
  public void update(boolean down, boolean eligible) {
    update(down, eligible, true);
  }
  public void update(boolean down, boolean eligible, boolean matches) {
    if (!eligible) { stop(); return; }
    if (down && !wasDown && matches) active = !active;
    wasDown = down;
  }
  public boolean active() { return active; }
  public void stop() { active = false; wasDown = true; }
}
