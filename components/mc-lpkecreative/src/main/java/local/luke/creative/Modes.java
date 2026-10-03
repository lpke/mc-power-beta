package local.luke.creative;

import local.luke.creative.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.maths.Box;

public final class Modes {
  private static PlayerEntity savedPlayer;
  private static net.minecraft.level.Level savedLevel;
  private static double savedX, savedY, savedZ;

  private Modes() {}

  public static boolean spectator(Object player) {
    return player instanceof ModePlayer p && p.lpke_isSpectator();
  }

  public static boolean protectedPlayer(Object player) {
    return player instanceof PlayerEntity p && (p.creative_isCreative() || spectator(p));
  }

  public static boolean canEnableFlight(Object player) {
    return player instanceof PlayerEntity p
        && p.creative_isCreative()
        && local.luke.creative.config.Config.current().flight
        && p.vehicle == null;
  }

  public static boolean flying(PlayerEntity player) {
    return player != null
        && !player.level.isRemote
        && player.vehicle == null
        && (spectator(player) || player.creative_isCreative() && player.creative_isFlying());
  }

  public static boolean change(Minecraft mc, GameMode mode) {
    if (!ClientRuntime.local(mc)) return false;
    PlayerEntity player = mc.player;
    ModePlayer state = (ModePlayer) player;
    if (state.lpke_mode() == mode) return true;
    if (spectator(player) && !findExit(player)) {
      mc.overlay.addChatMessage(
          "[LpkeCreative] No safe exit found. Move out of solid blocks first.");
      return false;
    }
    if (mode == GameMode.SPECTATOR) {
      savedPlayer = player;
      savedLevel = player.level;
      savedX = player.x;
      savedY = player.y;
      savedZ = player.z;
      if (player.vehicle != null) player.stopRiding(null);
      if (player.passenger != null) player.passenger.stopRiding(null);
    }
    state.lpke_applyMode(mode);
    FlightController.reset();
    mc.overlay.addChatMessage("Set own game mode to " + mode.label + " Mode");
    return true;
  }

  public static boolean findExit(PlayerEntity player) {
    if (clear(player, player.x, player.y, player.z)) return true;
    // Prefer the entry position when it still belongs to this world and remains clear.
    if (savedPlayer == player
        && savedLevel == player.level
        && clear(player, savedX, savedY, savedZ)) {
      player.setPosition(savedX, savedY, savedZ);
      return true;
    }
    double x = Math.floor(player.x) + .5, z = Math.floor(player.z) + .5;
    int start = (int) Math.floor(player.boundingBox.minY);
    for (int distance = 0; distance < 128; distance++) {
      for (int sign : new int[] {1, -1}) {
        int y = start + distance * sign;
        if (y < 1 || y > 126) continue;
        double eye = y + player.standingEyeHeight;
        if (clear(player, x, eye, z)) {
          player.setPosition(x, eye, z);
          return true;
        }
      }
    }
    return false;
  }

  private static boolean clear(PlayerEntity p, double x, double y, double z) {
    int bx = (int) Math.floor(x),
        by = (int) Math.floor(y - p.standingEyeHeight),
        bz = (int) Math.floor(z);
    if (by < 1 || by > 126 || !p.level.isBlockLoaded(bx, by, bz)) return false;
    Box box =
        Box.create(
            x - .3, y - p.standingEyeHeight, z - .3, x + .3, y - p.standingEyeHeight + 1.8, z + .3);
    return p.level.getCollidingEntities(p, box).isEmpty() && !p.level.containsLiquids(box);
  }

  public static void reset() {
    savedPlayer = null;
    savedLevel = null;
  }
}
