package local.luke.power.commands;

import java.lang.reflect.Method;
import local.luke.power.permissions.CommandPermissions;
import net.minecraft.client.Minecraft;

public final class CommandContext {
  private static final Method CREATIVE = creativeMethod();

  private static Method creativeMethod() {
    try {
      return Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
          .getMethod("creative_isCreative");
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static String world(Minecraft mc) {
    return mc.world == null || mc.world.isRemote
        ? ""
        : ((CommandWorld) mc.world.method_262()).power$commandWorldId();
  }

  public static CommandPermissions.Context current(Minecraft mc) {
    boolean creative = false;
    try {
      creative = mc.player != null && Boolean.TRUE.equals(CREATIVE.invoke(mc.player));
    } catch (ReflectiveOperationException ignored) {
      /* Fail closed if the mode cannot be read. */
    }
    return new CommandPermissions.Context(
        world(mc), creative, mc.world != null && mc.world.isRemote);
  }
}
