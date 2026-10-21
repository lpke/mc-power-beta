package local.luke.power.input;

/** Mode queries shared by modules using different game mappings. */
public interface PlayerMode {
  boolean power$creative();
  boolean power$spectator();
  /** Inventory-only capabilities. These must never authorize physical world interaction. */
  default boolean power$inventoryEditable() { return !power$spectator(); }
  default boolean power$creativeInventory() { return power$creative(); }
  default boolean power$flying() { return false; }
}
