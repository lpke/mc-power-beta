package local.luke.power.inventory;

/** Mapping-neutral access to the separate, durable offhand slot. */
public interface OffhandInventory<S> {
  S power$offhandStack();
  void power$offhandStack(S stack);
  /** Occupied slots remain visible and removable when insertion is disabled. */
  boolean power$offhandEnabled();
  void power$offhandChanged();
}
