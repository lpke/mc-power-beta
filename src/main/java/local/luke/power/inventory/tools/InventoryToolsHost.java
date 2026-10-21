package local.luke.power.inventory.tools;

public interface InventoryToolsHost {
  default InventoryToolsLayout.Rect power$recipeButtonBounds() { return null; }
  default InventoryToolsLayout.Rect power$inventoryToolsPlayerArea() { return null; }
  void power$installInventoryTools();
  boolean power$inventoryToolsModal();
}
