package local.luke.power.input;

/** Release ownership of a prior inventory gesture without moving any items. */
public interface InventoryDrag {
  void power$cancelItemDrag();
  /** Unknown gesture implementations cannot authorize a queued native publication. */
  default boolean power$itemDragCurrent(Object owner) { return false; }
  /** A flushed plan has verified these slot writes against the complete protected inventory. */
  default void power$acceptItemDragChanges(Object owner, java.util.List<?> slots) {}
}
