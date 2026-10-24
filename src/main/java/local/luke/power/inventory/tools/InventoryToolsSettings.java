package local.luke.power.inventory.tools;

public final class InventoryToolsSettings {
  public boolean sortDescending;
  public java.util.List<Integer> protectedSlots = new java.util.ArrayList<>();
  public java.util.Map<Integer, String> preferredItems = new java.util.LinkedHashMap<>();
  // Retained for old configuration snapshots; sorting and deposit no longer have enable switches.
  public boolean sorting, depositMatching;
  public boolean creativeHotbars;
  // Visit-local activation. Legacy saved values must never reactivate automation.
  public transient boolean restock, toolSwitch;
  public boolean sortingButton = true, depositMatchingButton = true, creativeHotbarsButton = true;
  public boolean restockBeforeEmpty = true, restockOtherHotbar, restockTools = true, toolsFromInventory = true;
  public int restockThreshold = 6, toolDurabilityReserve = 1;
  // The wildcard admits newly registered tools. An empty list deliberately allows none.
  public java.util.List<String> toolWhitelist = new java.util.ArrayList<>(java.util.List.of("*"));
  public void validate() {
    if (toolWhitelist == null || toolWhitelist.size() > 4096)
      throw new IllegalArgumentException("Invalid tool whitelist");
    for (String tool : toolWhitelist)
      if (tool == null || !tool.equals("*") && !tool.matches("[1-9][0-9]{0,8}"))
        throw new IllegalArgumentException("Tools must use registered numeric item IDs");
    if (toolWhitelist.contains("*") && toolWhitelist.size() != 1)
      throw new IllegalArgumentException("All tools cannot be combined with individual tools");
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
