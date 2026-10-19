package local.luke.power.commands.util;

import local.luke.power.commands.ClientCommands;
import local.luke.power.commands.api.Command;
import local.luke.power.permissions.CommandPermissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** The same command availability for the help index and completion. */
public final class CommandAccess {
  private CommandAccess() {}

  public static boolean showHelp(SharedCommandSource source, String input) {
    String[] args = input.trim().split("\\s+");
    if (args.length != 2 || !args[0].startsWith("/") || !args[1].equalsIgnoreCase("help")) return false;
    String name = args[0].substring(1).toLowerCase(java.util.Locale.ROOT);
    for (Command command : RetroChatUtil.commands) if (name.equals(command.name()) && available(command, source)) {
      command.manual(source);
      return true;
    }
    return false;
  }

  public static boolean operator(SharedCommandSource source) {
    if (!source.isClient()) return source.getPlayer() == null || ServerUtil.isOp(source.getName());
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    return mc.world != null && (!mc.world.isRemote || ClientCommands.mp_op);
  }

  public static boolean available(Command command, SharedCommandSource source) {
    if (!command.available(source) || command.needsPermissions() && !operator(source)) return false;
    if (source.isClient()) {
      Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
      if (mc.world == null || !mc.world.isRemote && command.disableInSingleplayer()
          || mc.world.isRemote && ClientCommands.disabled_commands.contains(command.name())) return false;
      if (mc.world.isRemote && !ClientCommands.mp_rc && !java.util.Set.of(
          "help", "perm", "clearchat", "waypoint", "way", "wp", "/calc", "tell", "me", "kill", "kick", "ban", "pardon",
          "ban-ip", "pardon-ip", "op", "deop", "stop", "list", "say", "whitelist", "save-all", "save-on", "save-off", "give", "time", "tp")
          .contains(command.name())) return false;
    }
    // Mixed commands retain their safe subcommands.
    return CommandPermissions.canonical(command.name()).equals("gamemode")
        || CommandPermissions.available(command.name());
  }
}
