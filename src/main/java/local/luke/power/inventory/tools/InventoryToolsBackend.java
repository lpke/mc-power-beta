package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class InventoryToolsBackend implements Backend {
  public static void register(ConfigSession session) throws ReflectiveOperationException {
    var current = InventoryToolsConfig.current(); var defaults = new InventoryToolsSettings();
    List<Setting> entries = new ArrayList<>();
    for (var field : InventoryToolsSettings.class.getFields()) {
      String key = field.getName();
      String label = switch (key) {
        case "sorting" -> "Inventory sorting";
        case "sortDescending" -> "Reverse item order";
        case "protectedSlots" -> "Protected storage slots";
        case "preferredItems" -> "Sorting layout";
        case "depositMatching" -> "Deposit matching items";
        case "creativeHotbars" -> "Saved Creative hotbars";
        case "restock" -> "Automatic hotbar restocking";
        case "restockBeforeEmpty" -> "Refill before empty";
        case "restockThreshold" -> "Refill at item count";
        case "restockOtherHotbar" -> "Refill from other hotbar slots";
        case "restockTools" -> "Replace broken tools";
        case "toolSwitch" -> "Automatic tool switching";
        case "toolsFromInventory" -> "Find tools in main storage";
        default -> "Tool durability reserve";
      };
      String help = switch (key) {
        case "sortDescending" -> "Sort higher item IDs first. Preferred slot items and protected slots keep their positions.";
        case "protectedSlots" -> "Keep selected main-storage slots untouched by sorting and Deposit matching. Configure them in the sorting layout.";
        case "preferredItems" -> "Choose protected slots and preferred items in a main-storage layout. Preferences move only existing items, preserving all native data.";
        case "sorting" -> "Show sorting controls for native player storage, chests and dispensers in singleplayer. Keeps the hotbar, armor, offhand and crafting slots separate.";
        case "depositMatching" -> "Deposit main-storage items that match the chest or dispenser's original contents, including all native item data.";
        case "creativeHotbars" -> "Save nine hotbar presets and load them in singleplayer Creative. Disabling keeps saved presets.";
        case "restock" -> "Refill the hand after native item use or a successful break consumes its item. Manual drops and menu edits never trigger refilling. Singleplayer Survival only.";
        case "restockBeforeEmpty" -> "Top up a stack after using an item when its remaining count reaches the refill threshold. Off waits until empty.";
        case "restockThreshold" -> "A lower count refills later and moves items less often. A higher count refills sooner after using an item. Never changes an untouched hand.";
        case "restockOtherHotbar" -> "Also take matching items from other hotbar slots. Off uses only the three main-storage rows.";
        case "restockTools" -> "After a successful native action breaks a tool, equip another matching tool, preserving its native data. The most worn matching tool is used first.";
        case "toolSwitch" -> "Before mining, choose a faster suitable tool using native Beta harvestability and mining strength. Keeps the current tool on ties and preserves torch-shovel priority. Singleplayer Survival only.";
        case "toolsFromInventory" -> "Allow tools from the three main-storage rows to swap into the selected hotbar slot. Off only selects tools already in the hotbar.";
        default -> "Do not automatically choose tools with this many durability uses left or fewer. Lower values use tools longer; higher values protect them earlier. Zero allows the final use.";
      };
      String group = key.equals("sorting") || key.equals("depositMatching") || key.equals("sortDescending") || key.equals("protectedSlots") || key.equals("preferredItems") ? "Sorting"
          : key.equals("creativeHotbars") ? "Saved Creative hotbars" : key.startsWith("tool") ? "Automatic tool switching" : "Automatic restocking";
      boolean numeric = field.getType() == int.class;
      entries.add(new Setting("inventoryTools." + key, "inventoryTools", "Inventory", group, label, help,
          numeric ? Setting.Kind.INTEGER : field.getType() == boolean.class ? Setting.Kind.BOOLEAN : Setting.Kind.LIST, Catalog.JSON.toJsonTree(field.get(current)),
          Catalog.JSON.toJsonTree(field.get(defaults)), key.equals("restockThreshold") ? 1 : 0,
          key.equals("restockThreshold") ? 64 : key.equals("toolDurabilityReserve") ? 100 : 1, 1, List.of(), false));
    }
    entries.add(new Setting("inventoryTools.hotbarEditor", "inventoryTools", "Inventory", "Saved Creative hotbars",
        "Edit saved hotbars", "Edit saved Creative hotbars without changing your inventory. Requires Creative access in a singleplayer world.",
        Setting.Kind.TEXT, new JsonPrimitive("Edit"), new JsonPrimitive("Edit"), 0, 0, 1, List.of(), false));
    session.add(new InventoryToolsBackend(), entries);
  }
  public String id() { return "inventoryTools"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private InventoryToolsSettings draft(Map<String, JsonElement> values) throws ReflectiveOperationException {
    var next = InventoryToolsConfig.copy();
    for (var entry : values.entrySet()) {
      if (entry.getKey().equals("inventoryTools.hotbarEditor")) continue;
      if (!entry.getKey().startsWith("inventoryTools.")) throw new IllegalArgumentException("Unknown inventory setting");
      var field = InventoryToolsSettings.class.getField(entry.getKey().substring("inventoryTools.".length()));
      field.set(next, Catalog.JSON.fromJson(entry.getValue(), field.getGenericType()));
    }
    next.validate(); return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception { InventoryToolsConfig.preview(draft(values)); }
  public void apply(Map<String, JsonElement> values) throws Exception { InventoryToolsConfig.save(draft(values)); }
}
