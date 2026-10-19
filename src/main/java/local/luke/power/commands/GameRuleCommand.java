package local.luke.power.commands;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.chat.HelpOutput;
import local.luke.power.rules.*;
import local.luke.power.world.WorldRuleChanges;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Uses the shared command registry while retaining native world-save ownership. */
public final class GameRuleCommand {
  private GameRuleCommand() {}
  public static void register() {
    try {
      Class<?> api = Class.forName("local.luke.power.commands.api.Command");
      Object command = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
        return switch (method.getName()) {
          case "name" -> "gamerule";
          case "needsPermissions", "disableInSingleplayer" -> false;
          case "helpGroup" -> "";
          case "available" -> mc.world != null && !mc.world.isRemote;
          case "command" -> { execute(mc, (String[]) args[1]); yield null; }
          case "manual" -> { help(mc, 1, false); yield null; }
          case "suggestion" -> suggestions((Integer) args[1], (String) args[2], (String) args[3]);
          case "toString" -> "Game rule command";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> throw new UnsupportedOperationException(method.getName());
        };
      });
      Class.forName("local.luke.power.commands.api.CommandRegistry").getMethod("add", api).invoke(null, command);
    } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not register /gamerule", e); }
  }
  public static String[] suggestions(int index, String prefix, String total) {
    List<String> values = new ArrayList<>();
    if (index == 1) {
      values.add("help");
      for (GameRule rule : GameRule.values()) { values.add(rule.id()); values.add(rule.alias()); }
    } else if (index == 2) {
      String[] words = total.strip().split("\\s+");
      if (words.length > 1 && words[1].equalsIgnoreCase("help")) values.addAll(java.util.stream.IntStream.rangeClosed(1, pages()).mapToObj(Integer::toString).toList());
      GameRule rule = words.length > 1 ? GameRule.find(words[1]) : null;
      if (rule != null) values.addAll(rule.integer() ? List.of("0", "100") : List.of("true", "false"));
    }
    return values.stream().filter(v -> local.luke.power.chat.ChatCompletion.startsWith(v, prefix)).sorted().map(v -> v.substring(prefix.length())).toArray(String[]::new);
  }
  public static void execute(Minecraft mc, String[] args) throws Exception {
    if (mc.world == null || mc.world.isRemote) throw new IllegalArgumentException("Game rules belong to the server. Open a singleplayer world to edit local rules.");
    if (args.length == 1) { help(mc, 1, false); return; }
    if (args[1].equalsIgnoreCase("help")) {
      if (args.length > 3) throw new IllegalArgumentException("Usage: /gamerule help [page]");
      int page = 1;
      if (args.length == 3) {
        try { page = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Choose a valid game-rule help page."); }
      }
      help(mc, page, args.length == 3); return;
    }
    if (args.length > 3) throw new IllegalArgumentException("Usage: /gamerule <rule> [value]");
    GameRule rule = GameRule.find(args[1]);
    if (rule == null) throw new IllegalArgumentException("Unknown game rule: " + args[1] + ". Use /gamerule help.");
    RuleSet before = WorldRules.of(mc.world.method_262()).copy();
    if (args.length == 2) { chat(mc, "§b" + rule.id() + " §7= §f" + before.get(rule)); return; }
    RuleSet next = before.copy(); next.set(rule, rule.parse(args[2]));
    WorldRuleChanges.validate(mc, next);
    try (var backup = local.luke.power.config.FileTransaction.begin(
        FabricLoader.getInstance().getGameDir(), WorldRuleChanges.files(mc))) {
      try { WorldRuleChanges.apply(mc, next); WorldRuleChanges.save(mc); backup.commit(); }
      catch (Exception failure) {
        try { WorldRuleChanges.apply(mc, before); }
        catch (Exception rollback) { failure.addSuppressed(rollback); }
        throw failure;
      }
    }
    chat(mc, "§a" + rule.id() + " §7= §f" + next.get(rule));
  }
  private static int pages() { return (GameRule.values().length + 6) / 7; }
  private static void help(Minecraft mc, int page, boolean explicitPage) {
    if (page < 1 || page > pages()) throw new IllegalArgumentException("Choose a valid game-rule help page.");
    chat(mc, "§b/gamerule <rule> [value] §7Read or change a rule for this world.");
    chat(mc, "§7Use true/false, or a percentage from 0 to 100 for numeric rules.");
    chat(mc, "§7Rule aliases work too. The local owner can always edit rules.");
    List<String> names = Arrays.stream(GameRule.values()).map(GameRule::id).sorted().toList();
    // Keep the first view short when chat scrolling is disabled.
    HelpOutput.print(line -> chat(mc, line), "Game rules", names, page, 7, !explicitPage && HelpOutput.scrolling());
    if (explicitPage || !HelpOutput.scrolling()) chat(mc, "§7/gamerule help <1–" + pages() + "> lists the other pages.");
    chat(mc, "§7Options > Game rules lists every rule with descriptions and defaults.");
  }
  private static void chat(Minecraft mc, String message) { mc.inGameHud.addChatMessage(message + "§r"); }
}
