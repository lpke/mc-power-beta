package local.luke.power.commands.command.vanilla;

import static local.luke.power.commands.util.ParameterSuggestUtil.suggestItemIdentifier;

import java.util.Optional;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.SharedCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.util.Identifier;

public class Give implements Command {

  public static boolean givePlayerItemInstance(
      SharedCommandSource commandSource, PlayerEntity player, ItemStack instance) {
    ItemStack[] inventory = player.inventory.main;
    for (int i = 0; i < inventory.length; i++) {
      if (inventory[i] == null) {
        inventory[i] = instance;
        return true;
      }
    }

    commandSource.sendFeedback(
        "Cannot give " + instance.getItem().getTranslatedName() + " because inventory is full");
    return false;
  }

  public static int nameToItemId(String n) {
    String name = n.replace("_", "");
    for (int i = 0; i < Item.ITEMS.length; i++) {
      if (Item.ITEMS[i] == null) continue;
      String translatedName = Item.ITEMS[i].getTranslatedName().replace(" ", ""); // Remove spaces
      if (translatedName.equalsIgnoreCase(name)) {
        return i;
      }
    }
    return -1;
  }

  public static int identifierToItemId(String n) {
    Optional<Item> item = ItemRegistry.INSTANCE.getOrEmpty(Identifier.of(n));
    return item.map(itemBase -> itemBase.id).orElse(-1);
  }

  @Override
  public void command(SharedCommandSource source, String[] args) {
    if (args.length < 3 || args.length > 5) {
      manual(source);
      return;
    }
    var targets = local.luke.power.commands.util.EntityTargets.resolve(source, args[1], true);
    var item = local.luke.power.commands.util.ItemArgument.parse(args[2]);
    int count =
        args.length > 3
            ? local.luke.power.commands.util.CommandNumbers.integer(args[3], 1, 2304, "Count")
            : 1;
    if (args.length == 5)
      item =
          new local.luke.power.commands.util.ItemArgument(
              item.item(),
              local.luke.power.commands.util.CommandNumbers.integer(args[4], 0, 32767, "Metadata"));
    // Validate the complete request before touching inventories. Split into valid stack sizes.
    for (var target : targets) {
      PlayerEntity player = (PlayerEntity) target;
      for (int remaining = count; remaining > 0; ) {
        int amount = Math.min(remaining, item.item().getMaxCount());
        ItemStack stack = item.stack(amount);
        player.inventory.addStack(stack);
        if (stack.count > 0) player.dropItem(stack);
        remaining -= amount;
      }
      player.inventory.markDirty();
    }
    source.sendFeedback(
        "§aGave "
            + count
            + " "
            + item.item().getTranslatedName()
            + " to "
            + targets.size()
            + " player(s).");
  }

  public String name() {
    return "give";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/give <targets> <item> [count=1] [metadata]");
    s.sendFeedback(
        "Names, minecraft:item names and Beta numeric IDs are accepted. Full inventories drop the"
            + " extra items nearby.");
  }

  public String[] suggestion(SharedCommandSource source, int n, String input, String total) {
    if (n == 1) return local.luke.power.commands.util.CommandSuggestions.targets(source, input);
    if (n == 2) return suggestItemIdentifier(input);
    return local.luke.power.commands.util.CommandSuggestions.suffix(
        input, n == 3 ? java.util.List.of("1", "64") : java.util.List.of());
  }
}
