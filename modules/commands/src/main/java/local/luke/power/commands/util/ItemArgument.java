package local.luke.power.commands.util;

import java.util.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.registry.ItemRegistry;

/** Plain registry names, explicit namespaces and Beta IDs/metadata are accepted. */
public record ItemArgument(Item item, int metadata) {
  public static ItemArgument parse(String token) {
    int meta = 0;
    int last = token.lastIndexOf(':');
    if (last >= 0 && token.substring(last + 1).matches("[0-9]+")) {
      meta = CommandNumbers.integer(token.substring(last + 1), 0, 32767, "Metadata");
      token = token.substring(0, last);
    }
    int id;
    if (token.matches("[+-]?[0-9]+"))
      id = CommandNumbers.integer(token, 0, Item.ITEMS.length - 1, "Item ID");
    else {
      id = identifierToItemId(token);
      if (id < 0 && !token.contains(":")) id = nameToItemId(token);
    }
    if (id < 0 || id >= Item.ITEMS.length || Item.ITEMS[id] == null)
      throw new IllegalArgumentException("Unknown Beta item: " + token);
    return new ItemArgument(Item.ITEMS[id], meta);
  }

  public ItemStack stack(int count) {
    return new ItemStack(item, count, metadata);
  }

  public static int identifierToItemId(String token) {
    return names().resolve(token);
  }

  /** Retain translated names as a fallback, without choosing between distinct matches. */
  public static int nameToItemId(String token) {
    String name = token.replace("_", "").replace(" ", "");
    int found = -1;
    for (Item item : Item.ITEMS) {
      if (item == null || !item.getTranslatedName().replace(" ", "").equalsIgnoreCase(name)) continue;
      if (found >= 0 && found != item.id)
        throw new IllegalArgumentException("Ambiguous item '" + token + "'. Use its full identifier or numeric ID.");
      found = item.id;
    }
    return found;
  }

  public static String[] suggestNames(String input) {
    return names().suggest(input);
  }

  private static ItemNames names() {
    Map<String, Integer> registered = new HashMap<>();
    for (var identifier : ItemRegistry.INSTANCE.getIds()) {
      Item item = ItemRegistry.INSTANCE.get(identifier);
      if (item != null) registered.put(identifier.toString(), item.id);
    }
    ItemNames names = new ItemNames(registered);
    for (Item item : Item.ITEMS) if (item != null) names.alias(item.getTranslatedName(), item.id);
    return names;
  }
}
