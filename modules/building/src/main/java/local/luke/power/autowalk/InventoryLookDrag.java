package local.luke.power.autowalk;

/** Only a qualifying press can start a drag; eligibility changes can only end it. */
public final class InventoryLookDrag {
  private Object screen, world, player;
  private boolean active;

  public boolean press(Object screen, Object world, Object player, boolean eligible) {
    stop();
    if (!eligible || screen == null || world == null || player == null) return false;
    this.screen = screen; this.world = world; this.player = player;
    active = true;
    return true;
  }

  public boolean update(Object screen, Object world, Object player, boolean down, boolean eligible) {
    if (!down || !eligible || this.screen != screen || this.world != world || this.player != player) stop();
    return active;
  }

  public boolean active() { return active; }
  public void stop() { active = false; screen = world = player = null; }
}
