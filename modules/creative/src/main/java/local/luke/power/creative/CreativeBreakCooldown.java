package local.luke.power.creative;

/** A new press may restart only the short delay from this player's successful Creative break. */
public final class CreativeBreakCooldown {
  private Object world, player;
  private int tick;

  public void broken(Object world, Object player, int tick) {
    this.world = world; this.player = player; this.tick = tick;
  }

  public void context(Object world, Object player, int tick) {
    if (world != this.world || player != this.player || tick < this.tick || tick - this.tick >= 5) clear();
  }

  public boolean restart(Object world, Object player, int tick, int cooldown) {
    return world != null && world == this.world && player == this.player
        && tick >= this.tick && tick - this.tick < 5 && cooldown > 0 && cooldown <= 5;
  }

  public void clear() { world = player = null; }
}
