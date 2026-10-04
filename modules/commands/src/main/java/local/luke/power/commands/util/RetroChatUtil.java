package local.luke.power.commands.util;

import java.util.ArrayList;
import java.util.Locale;
import local.luke.power.commands.ClientCommands;
import local.luke.power.commands.api.Command;
import local.luke.power.commands.command.extra.*;
import local.luke.power.commands.command.optional.Gamemode;
import local.luke.power.commands.command.server.*;
import local.luke.power.commands.command.vanilla.*;

public class RetroChatUtil {
  public static ArrayList<Command> commands = new ArrayList<>();

  public static void addDefaultCommands() {
    commands.add(new Help());
    commands.add(new Mods());

    commands.add(new Kick());
    commands.add(new Ban());
    commands.add(new Pardon());
    commands.add(new BanIp());
    commands.add(new PardonIp());
    commands.add(new Op());
    commands.add(new Deop());
    commands.add(new Stop());
    commands.add(new Save());
    commands.add(new List());
    commands.add(new Say());
    commands.add(new Whitelist());

    commands.add(new Clear());
    commands.add(new ClearChat());
    commands.add(new Weather());
    commands.add(new Difficulty());
    commands.add(new Seed());
    commands.add(new SpawnPoint());
    commands.add(new SetWorldSpawn());
    if (ClientCommands.creativeInventoryAvailable) {
      commands.add(new Gamemode());
    }
    commands.add(new Give());
    commands.add(new God());
    commands.add(new Heal());
    commands.add(new Id());
    commands.add(new Mobs());
    commands.add(new Summon());
    commands.add(new Tpa());
    commands.add(new Teleport());
    commands.add(
        new Teleport() {
          public String name() {
            return "teleport";
          }
        });
    commands.add(new Time());
    commands.add(new Clock());
    commands.add(new ToggleDownfall());
    commands.add(new Ride());
    commands.add(new Hat());
    commands.add(new KillAll());
    commands.add(new Kill());
    commands.add(new Warp());
    commands.add(new WhoAmI());
  }

  public static boolean handleCommand(SharedCommandSource source, String input, boolean operator) {
    if (input == null || input.isBlank()) return false;
    String[] args = input.trim().split("\\s+");
    String name = args[0].toLowerCase(Locale.ROOT);
    for (Command command : commands) {
      if (!name.equals(command.name())
          || !operator && command.needsPermissions()
          || source.isClient() && command.disableInSingleplayer()) continue;
      String denial =
          (name.equals("gamemode") || name.equals("gm"))
              ? ""
              : local.luke.power.permissions.CommandPermissions.denial(name);
      if (!denial.isEmpty()) {
        source.sendFeedback("§c" + denial);
        return true;
      }
      try {
        command.command(source, args);
      } catch (IllegalArgumentException | ArithmeticException e) {
        source.sendFeedback("§c" + e.getMessage());
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger("Power Beta commands")
            .error("Command failed: {}", name, e);
        source.sendFeedback("§cThe command could not finish. Check the game log.");
      }
      // A recognized command stays consumed even on failure, avoiding duplicate dispatch.
      return true;
    }
    source.sendFeedback("§cUnknown command: " + name + ". Use /help.");
    return false;
  }
}
