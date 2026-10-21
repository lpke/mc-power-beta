package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.IntPredicate;
import local.luke.power.death.DeathItems;
import local.luke.power.storage.PowerConfig;
import local.luke.power.world.WorldRuleChanges;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Read-only checks against the installed command modules and live item registry. */
final class CommandNames402Checks {
  private CommandNames402Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote,
        "open a disposable singleplayer world");
    failures = 0;
    String inventory = inventory(mc);
    int selected = mc.player.inventory.selectedSlot;
    var player = mc.player;
    var world = mc.world;
    var screen = mc.currentScreen;
    var container = mc.player.container;
    var preferences = PowerConfig.document().deepCopy();
    var rules = AccessRules.copy(mc).document();
    Map<Path, byte[]> files = new LinkedHashMap<>();
    files.put(PowerConfig.path(), bytes(PowerConfig.path()));
    for (Path path : WorldRuleChanges.files(mc)) files.put(path, bytes(path));
    try {
      test("COMMAND_NAMES402 live vanilla registry names retain numeric IDs and metadata", () -> {
        Object dirt = registered("minecraft:dirt");
        for (String token : List.of("dirt", "minecraft:dirt", "3")) {
          ItemStack stack = item(token, dirt, 0);
          check(stack.itemId == 3, "dirt resolved to a different numeric ID: " + token);
        }
        Object wool = registered("minecraft:wool");
        for (String token : List.of("wool:14", "minecraft:wool:14", "35:14"))
          check(item(token, wool, 14).itemId == 35, "wool metadata changed its item ID");
      });
      test("COMMAND_NAMES402 creative helper paths resolve without their namespace", () -> {
        for (String path : List.of("tall_grass", "fern")) {
          String qualified = "power_creative_inventory:" + path;
          Object expected = registered(qualified);
          ItemStack plain = item(path, expected, 0);
          item(qualified, expected, 0);
          item(path + ":2", expected, 2);
          item(qualified + ":2", expected, 2);
          item(plain.itemId + ":2", expected, 2);
        }
      });
      test("COMMAND_NAMES402 live and registered command completion suggests plain paths", () -> {
        Class<?> suggestions = Class.forName("local.luke.power.commands.util.ParameterSuggestUtil");
        Method suggest = suggestions.getMethod("suggestItemIdentifier", String.class);
        suffix((String[]) suggest.invoke(null, "di"), "rt", "di");
        suffix((String[]) suggest.invoke(null, "fe"), "rn", "fe");
        suffix((String[]) suggest.invoke(null, "tall_g"), "rass", "tall_g");
        check(Arrays.stream((String[]) suggest.invoke(null, "")).noneMatch(value -> value.contains(":")),
            "normal live suggestions include a namespace");
        suffix((String[]) suggest.invoke(null, "minecraft:di"), "rt", "minecraft:di");
        suffix((String[]) suggest.invoke(null, "power_creative_inventory:fe"), "rn", "power_creative_inventory:fe");

        Class<?> sourceType = Class.forName("local.luke.power.commands.util.SharedCommandSource");
        Object source = sourceType.getConstructor(Object.class).newInstance(mc.player);
        Class<?> commandType = Class.forName("local.luke.power.commands.api.Command");
        Method name = commandType.getMethod("name");
        Method complete = commandType.getMethod("suggestion", sourceType, int.class, String.class, String.class);
        var commands = (List<?>) Class.forName("local.luke.power.commands.util.RetroChatUtil")
            .getField("commands").get(null);
        for (String command : List.of("give", "clear", "id")) {
          Object registered = null;
          for (Object candidate : commands) if (command.equals(name.invoke(candidate))) { registered = candidate; break; }
          check(registered != null, "command was not registered: " + command);
          int parameter = command.equals("id") ? 1 : 2;
          String total = "/" + command + (parameter == 1 ? " di" : " @p di");
          String[] choices = (String[]) complete.invoke(registered, source, parameter, "di", total);
          suffix(choices, "rt", total);
          check(Arrays.stream(choices).noneMatch(value -> value.contains(":")),
              command + " inserted a namespace into ordinary completion");
        }
      });
      test("COMMAND_NAMES402 installed WorldEdit parser retains plain variants and states", () -> {
        Class<?> type = Class.forName("local.luke.power.worldedit.core.BlockParser");
        Object parser = type.getConstructor(IntPredicate.class).newInstance((IntPredicate)
            id -> id >= 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null);
        Method block = type.getMethod("block", String.class);
        for (String token : List.of("fern", "power_creative_inventory:fern", "31:2"))
          block(block.invoke(parser, token), 31, 2, token);
        for (String token : List.of("wool:14", "minecraft:wool:14", "35:14"))
          block(block.invoke(parser, token), 35, 14, token);
        block(block.invoke(parser, "minecraft:rail[shape=east_west]"), 66, 1, "rail state");
      });
    } finally {
      test("COMMAND_NAMES402 parsing and completion leave inventory and world settings unchanged", () -> {
        check(mc.player == player && mc.world == world && mc.currentScreen == screen
            && mc.player.container == container, "parse/completion changed a native owner or screen");
        check(inventory.equals(inventory(mc)) && selected == mc.player.inventory.selectedSlot,
            "parse/completion changed inventory, offhand, cursor, crafting inputs or selected slot");
        check(preferences.equals(PowerConfig.document()) && rules.equals(AccessRules.copy(mc).document()),
            "parse/completion changed global preferences or world rules");
        for (var entry : files.entrySet())
          check(Arrays.equals(entry.getValue(), bytes(entry.getKey())),
              "parse/completion wrote preferences or world metadata: " + entry.getKey().getFileName());
      });
    }
    log("COMMAND_NAMES402 failures=" + failures);
  }

  private static Object registered(String identifier) throws Exception {
    Class<?> identifiers = Class.forName("net.modificationstation.stationapi.api.util.Identifier");
    Object id = identifiers.getMethod("of", String.class).invoke(null, identifier);
    Object registry = Class.forName("net.modificationstation.stationapi.api.registry.ItemRegistry")
        .getField("INSTANCE").get(null);
    return ((Optional<?>) registry.getClass().getMethod("getOrEmpty", identifiers).invoke(registry, id))
        .orElseThrow(() -> new AssertionError("live registry is missing " + identifier));
  }

  private static ItemStack item(String token, Object expected, int metadata) throws Exception {
    Class<?> type = Class.forName("local.luke.power.commands.util.ItemArgument");
    Object parsed = type.getMethod("parse", String.class).invoke(null, token);
    check(type.getMethod("item").invoke(parsed) == expected, "wrong live item identity: " + token);
    check((int) type.getMethod("metadata").invoke(parsed) == metadata, "wrong metadata: " + token);
    ItemStack stack = (ItemStack) type.getMethod("stack", int.class).invoke(parsed, 1);
    check(stack.count == 1 && stack.getDamage() == metadata, "stack creation lost count or metadata: " + token);
    return stack;
  }

  private static void suffix(String[] choices, String expected, String input) {
    check(Arrays.asList(choices).contains(expected), "missing completion for " + input + ": " + Arrays.toString(choices));
  }

  private static void block(Object value, int id, int metadata, String token) throws Exception {
    check(value.getClass().getField("id").getInt(value) == id
        && value.getClass().getField("meta").getInt(value) == metadata, "wrong block variant: " + token);
  }

  private static String inventory(Minecraft mc) throws Exception {
    return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
  }

  private static byte[] bytes(Path path) throws Exception {
    return Files.exists(path) ? Files.readAllBytes(path) : null;
  }
}
