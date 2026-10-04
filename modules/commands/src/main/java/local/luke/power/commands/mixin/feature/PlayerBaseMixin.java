package local.luke.power.commands.mixin.feature;

import local.luke.power.commands.api.PlayerWarps;
import local.luke.power.commands.command.extra.God;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Priority 999 to cancel fire even when in creative, because creative also cancels here.
// CreativeInventory uses priority 1000
@Mixin(value = PlayerEntity.class, priority = 999)
public class PlayerBaseMixin implements PlayerWarps, local.luke.power.commands.api.ForcedSpawn {
  @Unique private boolean power$forcedSpawn;
  @Unique private float power$spawnAngle;

  public boolean power$forcedSpawn() {
    return power$forcedSpawn;
  }

  public float power$spawnAngle() {
    return power$spawnAngle;
  }

  public void power$forcedSpawn(boolean enabled, float angle) {
    power$forcedSpawn = enabled;
    power$spawnAngle = angle;
  }

  @Inject(method = "setSpawnPos", at = @At("HEAD"))
  private void power$normalSpawn(net.minecraft.util.math.Vec3i pos, CallbackInfo ci) {
    power$forcedSpawn = false;
  }

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  public void onGod(Entity i, int par2, CallbackInfoReturnable<Boolean> cir) {
    PlayerEntity player = (PlayerEntity) (Object) this;

    if (God.isPlayerInvincible.containsKey(player.name))
      if (God.isPlayerInvincible.get(player.name)
          && local.luke.power.permissions.CommandPermissions.allowed("god")) {
        if (player.fireTicks > 0) player.fireTicks = 0;

        cir.cancel();
      }
  }

  @Unique String warpStr = "";

  @Override
  public void spc$setWarpString(String warp) {
    warpStr = warp;
  }

  @Override
  public String spc$getWarpString() {
    return warpStr;
  }

  @Inject(method = "readNbt", at = @At("TAIL"))
  private void readFromTag(NbtCompound tag, CallbackInfo ci) {
    power$forcedSpawn = tag.getBoolean("PowerBetaForcedSpawn");
    power$spawnAngle = tag.getFloat("PowerBetaSpawnAngle");
    if (!Float.isFinite(power$spawnAngle)) {
      power$forcedSpawn = false;
      power$spawnAngle = 0;
    }
    if (tag.contains("warps")) warpStr = tag.getString("warps");
  }

  @Inject(method = "writeNbt", at = @At("TAIL"))
  private void writeToTag(NbtCompound tag, CallbackInfo ci) {
    tag.putBoolean("PowerBetaForcedSpawn", power$forcedSpawn);
    tag.putFloat("PowerBetaSpawnAngle", power$spawnAngle);
    if (!warpStr.isEmpty()) tag.putString("warps", warpStr);
  }
}
