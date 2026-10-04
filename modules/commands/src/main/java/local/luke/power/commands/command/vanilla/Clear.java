package local.luke.power.commands.command.vanilla;

import local.luke.power.commands.api.Command;
import local.luke.power.commands.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class Clear implements Command {
  public void command(SharedCommandSource source, String[] args) {
    if (args.length > 4) {
      manual(source);
      return;
    }
    var targets = EntityTargets.resolve(source, args.length > 1 ? args[1] : "@s", true);
    ItemArgument filter = args.length > 2 ? ItemArgument.parse(args[2]) : null;
    boolean metadata = args.length > 2 && args[2].matches(".*:[0-9]+");
    int limit =
        args.length > 3
            ? CommandNumbers.integer(args[3], 0, Integer.MAX_VALUE, "Maximum count")
            : Integer.MAX_VALUE;
    int total = 0;
    // All selectors, IDs and counts are valid before intentional removal starts.
    for (var target : targets) {
      PlayerEntity player = (PlayerEntity) target;
      int count = 0;
      for (int slot = 0; slot < player.inventory.size(); slot++) {
        ItemStack stack = player.inventory.getStack(slot);
        if (stack == null
            || filter != null
                && (stack.itemId != filter.item().id
                    || metadata && stack.getDamage() != filter.metadata())) continue;
        if (limit == 0) {
          count += stack.count;
          continue;
        }
        int remove = Math.min(stack.count, limit - count);
        if (remove <= 0) break;
        ItemStack remaining = stack.copy();
        remaining.count -= remove;
        player.inventory.setStack(slot, remaining.count == 0 ? null : remaining);
        count += remove;
      }
      ItemStack cursor = player.inventory.getCursorStack();
      if (cursor != null
          && (filter == null
              || cursor.itemId == filter.item().id
                  && (!metadata || cursor.getDamage() == filter.metadata()))) {
        int remove = limit == 0 ? cursor.count : Math.min(cursor.count, limit - count);
        count += remove;
        if (limit != 0 && remove > 0) {
          ItemStack remaining = cursor.copy();
          remaining.count -= remove;
          player.inventory.setCursorStack(remaining.count == 0 ? null : remaining);
        }
      }
      if (limit != 0) player.inventory.markDirty();
      total += count;
    }
    source.sendFeedback("§a" + (limit == 0 ? "Found " : "Removed ") + total + " items.");
  }

  public String name() {
    return "clear";
  }

  public void manual(SharedCommandSource s) {
    s.sendFeedback("/clear [targets] [item] [maxCount]");
    s.sendFeedback(
        "Removes matching inventory items. maxCount 0 only counts them. /clearchat clears chat"
            + " instead.");
  }

  public String[] suggestion(SharedCommandSource s, int n, String input, String total) {
    return n == 1
        ? CommandSuggestions.targets(s, input)
        : n == 2 ? ParameterSuggestUtil.suggestItemIdentifier(input) : new String[0];
  }
}
