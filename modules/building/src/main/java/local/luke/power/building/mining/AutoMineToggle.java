package local.luke.power.building.mining;

/** A canceled action requires a fresh press, including after focus/menu changes. */
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
  /** Maintenance never turns a fresh physical edge into gameplay. */
  public void poll(boolean down, boolean eligible) {
    if (!eligible) stop();
    if (!down) wasDown = false;
  }
  public void press(boolean eligible, boolean matches) {
    if (!eligible) { stop(); return; }
    if (!wasDown && matches) active = !active;
    wasDown = true;
  }
  public void release() { wasDown = false; }
  public boolean active() { return active; }
  public void stop() { active = false; wasDown = true; }
}
