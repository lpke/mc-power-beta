package local.luke.power.commands.util;

import local.luke.power.commands.command.vanilla.Give;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Beta IDs/metadata remain accepted alongside namespaced names. */
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
      try {
        id = Give.identifierToItemId(token);
      } catch (RuntimeException e) {
        id = -1;
      }
      if (id < 0) id = Give.nameToItemId(token.replaceFirst("^minecraft:", ""));
    }
    if (id < 0 || id >= Item.ITEMS.length || Item.ITEMS[id] == null)
      throw new IllegalArgumentException("Unknown Beta item: " + token);
    return new ItemArgument(Item.ITEMS[id], meta);
  }

  public ItemStack stack(int count) {
    return new ItemStack(item, count, metadata);
  }
}
