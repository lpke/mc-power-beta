package local.luke.power.inventory.tools;

public final class InventoryToolsSettings {
  public boolean sorting, depositMatching, creativeHotbars, restock, toolSwitch;
  public boolean restockBeforeEmpty = true, restockOtherHotbar, restockTools = true, toolsFromInventory = true;
  public int restockThreshold = 6, toolDurabilityReserve = 1;
  public void validate() {
    if (restockThreshold < 1 || restockThreshold > 64 || toolDurabilityReserve < 0 || toolDurabilityReserve > 100)
      throw new IllegalArgumentException("Invalid restock threshold or tool durability reserve");
  }
}
