package local.luke.power.inventory.tools;

import java.util.*;
import net.minecraft.Item;
import net.minecraft.class_500;
import net.minecraft.item.ItemStack;

/** The wildcard keeps default switching compatible with tools registered later. */
public final class ToolWhitelist {
  private ToolWhitelist() {}
  public static boolean allows(List<String> whitelist, ItemStack stack) {
    return stack != null && stack.count > 0
        && (whitelist.contains("*") || whitelist.contains(Integer.toString(stack.itemId)));
  }
  public static boolean tool(Item item) {
    return item != null && item.getMaxDamage() > 0 && !(item instanceof class_500);
  }
  public static List<String> registered() {
    List<String> result = new ArrayList<>();
    for (int id = 1; id < Item.ITEMS.length; id++) if (tool(Item.ITEMS[id])) result.add(Integer.toString(id));
    return List.copyOf(result);
  }
}
