package local.luke.power.input;

/** Stable saved-world ownership shared by modules with different Minecraft mappings. */
public interface WorldIdentity {
  String power$commandWorldId();
  /** Transient proof published only after the load boundary verifies durable compatibility metadata. */
  default boolean power$inventoryReady() { return false; }
  default void power$inventoryReady(boolean ready) { throw new IllegalStateException("Missing world inventory guard"); }
}
