package local.luke.power.creative;

import local.luke.power.creative.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;

public final class Modes {
  private Modes() {}

  public static boolean spectator(Object player) {
    return player instanceof ModePlayer p && p.power_isSpectator();
  }

  public static boolean protectedPlayer(Object player) {
    return player instanceof PlayerEntity p && (p.creative_isCreative() || spectator(p));
  }

  public static boolean canEnableFlight(Object player) {
    return player instanceof PlayerEntity p
        && p.creative_isCreative()
        && local.luke.power.creative.config.Config.current().flight
        && p.vehicle == null;
  }

  public static boolean flying(PlayerEntity player) {
    return player != null
        && !player.level.isRemote
        && player.vehicle == null
        && (spectator(player) || player.creative_isCreative() && player.creative_isFlying());
  }

  public static String denial(GameMode mode) {
    return local.luke.power.permissions.CommandPermissions.modeDenial(mode.name().toLowerCase(java.util.Locale.ROOT));
  }

  public static void disableCheats(Minecraft mc) {
    if (mc.player != null && ((ModePlayer) mc.player).power_mode() == GameMode.SURVIVAL) return;
    if (!ClientRuntime.local(mc) || !change(mc, GameMode.SURVIVAL))
      throw new IllegalArgumentException("Place any carried container before disabling cheats.");
  }

  public static boolean change(Minecraft mc, GameMode mode) {
    if (!ClientRuntime.local(mc)) return false;
    String denied = denial(mode);
    if (!denied.isEmpty()) { mc.overlay.addChatMessage("§c" + denied + "§r"); return false; }
    PlayerEntity player = mc.player;
    ModePlayer state = (ModePlayer) player;
    if (state.power_mode() == mode) return true;
    if (local.luke.power.input.InteractionState.carryingContainer) {
      mc.overlay.addChatMessage("§cPlace your carried container before changing game mode.§r");
      return false;
    }
    if (mode == GameMode.SPECTATOR) {
      if (player.vehicle != null) player.stopRiding(null);
      if (player.passenger != null) player.passenger.stopRiding(null);
    }
    state.power_applyMode(mode);
    FlightController.reset();
    mc.overlay.addChatMessage("\u00a77Set own game mode to \u00a7a" + mode.label + "\u00a77 Mode\u00a7r");
    return true;
  }

}
