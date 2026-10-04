package local.luke.power.commands.mixin.client;

import local.luke.power.commands.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class CommandRespawnMixin {
  @Unique private boolean power$forced;
  @Unique private float power$angle;
  @Unique private String power$warps;

  @Inject(method = "respawnPlayer", at = @At("HEAD"))
  private void power$remember(boolean reset, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    power$forced = false;
    power$warps = null;
    if (mc.player == null || mc.world == null || mc.world.isRemote) return;
    ForcedSpawn data = (ForcedSpawn) mc.player;
    power$forced = !reset && data.power$forcedSpawn();
    power$angle = data.power$spawnAngle();
    power$warps = ((PlayerWarps) mc.player).spc$getWarpString();
  }

  @Redirect(
      method = "respawnPlayer",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/entity/player/PlayerEntity;findRespawnPosition(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3i;)Lnet/minecraft/util/math/Vec3i;"))
  private Vec3i power$position(World world, Vec3i pos) {
    if (power$forced
        && world.dimension.id == 0
        && pos.y >= 0
        && pos.y <= 126
        && Math.abs((long) pos.x) < 30000000
        && Math.abs((long) pos.z) < 30000000) return pos;
    power$forced = false;
    return PlayerEntity.findRespawnPosition(world, pos);
  }

  @Inject(method = "respawnPlayer", at = @At("RETURN"))
  private void power$restore(boolean reset, int dimension, CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    if (mc.player != null && mc.world != null && !mc.world.isRemote) {
      if (power$warps != null) ((PlayerWarps) mc.player).spc$setWarpString(power$warps);
      if (power$forced) {
        ((ForcedSpawn) mc.player).power$forcedSpawn(true, power$angle);
        mc.player.yaw = mc.player.prevYaw = power$angle;
      }
    }
    power$forced = false;
    power$warps = null;
  }
}
