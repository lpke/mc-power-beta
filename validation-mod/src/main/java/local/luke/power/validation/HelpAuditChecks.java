package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.commands.ClientCommands;
import local.luke.power.commands.command.vanilla.Help;
import local.luke.power.commands.util.*;
import local.luke.power.death.*;
import local.luke.power.permissions.*;
import net.minecraft.client.Minecraft;

final class HelpAuditChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var before = CommandPermissions.copy(); var death = DeathConfig.copy();
    var world = (CheatWorld)mc.world.method_262(); boolean cheats = world.power$cheatsEnabled();
    boolean op = ClientCommands.mp_op, server = ClientCommands.mp_rc; var disabled = ClientCommands.disabled_commands;
    List<String> output = new ArrayList<>();
    var source = new SharedCommandSource(mc.player) { @Override public void sendFeedback(String text) { output.add(text); } };
    var help = new Help();
    mc.setScreen(null); mc.player.container = mc.player.playerContainer;
    try {
      test("help reflects every non-cheat and cheat command plus aliases", () -> {
        for (boolean allowed : new boolean[]{false, true}) {
          world.power$cheatsEnabled(allowed); var settings = new DeathSettings(); settings.enabled = allowed; DeathConfig.preview(settings);
          Set<String> listed = names(help.lines(source));
          for (var command : RetroChatUtil.commands) {
            boolean available = CommandAccess.available(command, source);
            if (!command.helpGroup().isEmpty() && !command.name().equals("/help")) continue;
            check(listed.contains("/" + command.name()) == available, "help disagrees for " + command.name() + " cheats=" + allowed);
          }
          check(listed.contains("/gamemode") && listed.contains("/gm"), "gamemode aliases missing");
          check(listed.contains("/give") == allowed && listed.contains("/tp") == allowed && listed.contains("/teleport") == allowed, "cheat aliases wrong");
          check(listed.contains("/death") == allowed, "optional death command wrong");
          check(listed.contains("//help") && !listed.contains("//set") && !listed.contains("/undo"), "WorldEdit not represented once");
          check(listed.contains("//calc"), "independent calculator omitted");
        }
      });
      test("global rules, world overrides and disabled features filter help and completion", () -> {
        world.power$cheatsEnabled(true);
        var rules = CommandPermissions.copy(); rules.rules.put("give", CommandPermissions.Rule.DISABLED); rules.rules.put("seed", CommandPermissions.Rule.DISABLED);
        CommandPermissions.preview(rules); var listed = names(help.lines(source));
        check(!listed.contains("/give") && !listed.contains("/seed"), "disabled commands listed");
        var complete = Arrays.asList(help.suggestion(source, 1, "", "")); check(!complete.contains("give") && !complete.contains("seed"), "disabled help completion");
        String id = local.luke.power.commands.CommandContext.world(mc);
        rules.worlds.computeIfAbsent(id, k -> new LinkedHashMap<>()).put("give", CommandPermissions.Override.ALLOW);
        CommandPermissions.preview(rules); check(names(help.lines(source)).contains("/give"), "world allow ignored");
        rules.worlds.get(id).put("give", CommandPermissions.Override.BLOCK); CommandPermissions.preview(rules);
        check(!names(help.lines(source)).contains("/give"), "world block ignored");
        DeathConfig.preview(new DeathSettings()); check(!names(help.lines(source)).contains("/death"), "disabled death listed");
        CommandPermissions.preview(before);
      });
      test("every available command supports help without gameplay changes", () -> {
        world.power$cheatsEnabled(true); var settings = new DeathSettings(); settings.enabled = true; DeathConfig.preview(settings);
        var rules = CommandPermissions.copy(); rules.enabled = true; rules.rules.clear(); rules.worlds.clear(); CommandPermissions.preview(rules);
        String items = DeathItems.inventory(mc.player.inventory.main); long time = mc.world.getTime(); int health = mc.player.health;
        double x = mc.player.x, y = mc.player.y, z = mc.player.z;
        for (var command : RetroChatUtil.commands) if (CommandAccess.available(command, source)) {
          output.clear(); check(RetroChatUtil.handleCommand(source, command.name() + " help", true), "help dispatch failed: " + command.name());
          check(output.stream().noneMatch(s -> s.startsWith("§c")), "help error for " + command.name() + ": " + output);
          check(items.equals(DeathItems.inventory(mc.player.inventory.main)) && mc.world.getTime() == time && mc.player.health == health
              && mc.player.x == x && mc.player.y == y && mc.player.z == z, "help changed gameplay: " + command.name());
        }
        for (String command : List.of("/gm help", "//calc help", "//set help", "/undo help", "//help help")) {
          if (command.startsWith("/gm")) mc.player.sendChatMessage(command);
          else Class.forName("local.luke.power.worldedit.WorldEditor").getMethod("command", Minecraft.class, String.class).invoke(null, mc, command);
          check(items.equals(DeathItems.inventory(mc.player.inventory.main)) && mc.world.getTime() == time, "direct help changed gameplay: " + command);
        }
      });
      test("multiplayer operator, server support and disabled lists restrict help", () -> {
        mc.world.isRemote = true;
        try {
          ClientCommands.mp_rc = false; ClientCommands.mp_op = false;
          var listed = names(help.lines(source));
          check(listed.contains("/tell") && listed.contains("/me") && listed.contains("/perm"), "native multiplayer commands missing");
          for (String command : List.of("/tell help", "/me help", "/perm help", "/clearchat help")) {
            output.clear(); check(CommandAccess.showHelp(source, command) && !output.isEmpty(), "local multiplayer help missing: " + command);
          }
          check(!CommandAccess.showHelp(source, "/give help") && !CommandAccess.showHelp(source, "/me hello"), "unavailable help or ordinary command intercepted");
          check(!listed.contains("/give") && !listed.contains("/death") && !listed.contains("/warp") && !listed.contains("//help"), "unavailable multiplayer commands listed");
          ClientCommands.mp_op = true; check(names(help.lines(source)).contains("/give"), "operator commands missing");
          check(CommandAccess.showHelp(source, "/give help"), "operator help was forwarded to native server");
          ClientCommands.mp_rc = true; ClientCommands.disabled_commands = List.of("give");
          check(!names(help.lines(source)).contains("/give"), "server disabled command listed");
        } finally { mc.world.isRemote = false; }
      });
    } finally {
      CommandPermissions.preview(before); DeathConfig.preview(death); world.power$cheatsEnabled(cheats);
      ClientCommands.mp_op = op; ClientCommands.mp_rc = server; ClientCommands.disabled_commands = disabled;
    }
    log("HELP AUDIT FAILURES " + failures);
  }
  private static Set<String> names(List<String> lines) {
    Set<String> names = new HashSet<>(); for (String line : lines) names.add(line.split(" ", 2)[0]); return names;
  }
}
