package local.luke.power.world;

/** Per-world inventory retention, independent of access permissions. */
public interface WorldInventory {
  boolean power$keepInventory();
  void power$keepInventory(boolean enabled);
}
