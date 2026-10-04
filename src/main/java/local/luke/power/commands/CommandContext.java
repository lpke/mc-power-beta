package local.luke.power.commands;

import local.luke.power.permissions.CheatWorld;
import local.luke.power.permissions.CommandPermissions;
import net.minecraft.client.Minecraft;

public final class CommandContext {
  private CommandContext() {}

  public static String world(Minecraft mc) {
    return mc.world == null || mc.world.isRemote
        ? ""
        : ((CommandWorld) mc.world.method_262()).power$commandWorldId();
  }

  public static CommandPermissions.Context current(Minecraft mc) {
    return new CommandPermissions.Context(
        world(mc),
        mc.world != null && mc.world.isRemote,
        mc.world != null
            && !mc.world.isRemote
            && ((CheatWorld) mc.world.method_262()).power$cheatsEnabled());
  }
}
