package local.luke.power.inventory.tools;

public final class InventoryToolsSettings {
  public boolean sortDescending;
  public java.util.List<Integer> protectedSlots = new java.util.ArrayList<>();
  public java.util.Map<Integer, String> preferredItems = new java.util.LinkedHashMap<>();
  public boolean sorting, depositMatching, creativeHotbars, restock, toolSwitch;
  public boolean sortingButton = true, depositMatchingButton = true, creativeHotbarsButton = true;
  public boolean restockBeforeEmpty = true, restockOtherHotbar, restockTools = true, toolsFromInventory = true;
  public int restockThreshold = 6, toolDurabilityReserve = 1;
  public void validate() {
    if (protectedSlots == null || protectedSlots.size() > 27 || preferredItems == null || preferredItems.size() > 27)
      throw new IllegalArgumentException("Invalid sorting layout");
    for (Integer slot : protectedSlots)
      if (slot == null || slot < 9 || slot > 35) throw new IllegalArgumentException("Protected slots must be in main storage");
    for (var preference : preferredItems.entrySet())
      if (preference.getKey() == null || preference.getKey() < 9 || preference.getKey() > 35
          || preference.getValue() == null || !preference.getValue().matches("[1-9][0-9]{0,8}(:[0-9]{1,5})?"))
        throw new IllegalArgumentException("Invalid preferred slot item");
    if (restockThreshold < 1 || restockThreshold > 64 || toolDurabilityReserve < 0 || toolDurabilityReserve > 100)
      throw new IllegalArgumentException("Invalid restock threshold or tool durability reserve");
  }
}
