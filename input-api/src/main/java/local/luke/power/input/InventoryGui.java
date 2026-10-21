package local.luke.power.input;

/** Shared GUI hit testing across the building and creative modules' mappings. */
public interface InventoryGui {
  boolean power$containsGuiSpace(int x, int y);
  default boolean power$extraGuiSpace(int x, int y) { return false; }
  default boolean power$sideSlotGuiSpace(int x, int y) { return false; }
  default boolean power$usesCatalogWheel() { return false; }
  default boolean power$ownsPointerControl(int x, int y) { return false; }
}
