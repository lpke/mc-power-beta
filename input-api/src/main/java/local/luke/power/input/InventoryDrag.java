package local.luke.power.input;

/** Release ownership of a prior inventory gesture without moving any items. */
public interface InventoryDrag {
  void power$cancelItemDrag();
}
