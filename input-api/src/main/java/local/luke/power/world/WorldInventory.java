package local.luke.power.world;

/** A per-world rule which clears when cheats are disabled. */
public interface WorldInventory {
  boolean power$keepInventory();
  void power$keepInventory(boolean enabled);
}
