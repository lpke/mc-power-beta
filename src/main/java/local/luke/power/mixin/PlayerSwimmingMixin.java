package local.luke.power.mixin;

import local.luke.power.gameplay.*;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerSwimmingMixin implements SwimPlayer {
  @Unique private final SwimState power$swim = new SwimState();

  public SwimState power$swim() { return power$swim; }

  @Inject(method="method_938",at=@At("HEAD"))
  private void power$deathPose(Entity attacker,CallbackInfo ci) {
    if((Object)this instanceof ClientPlayerEntity player)ModernSwimming.died(player);
  }

  @Inject(method="method_1378",at=@At("HEAD"),cancellable=true)
  private void power$eye(CallbackInfoReturnable<Float> cir) {
    if (power$swim.compact || power$swim.crouched) cir.setReturnValue(0f);
  }

  @Inject(method="writeNbt",at=@At("TAIL"))
  private void power$savePose(NbtCompound nbt,CallbackInfo ci) {
    if ((Object)this instanceof ClientPlayerEntity player && !player.world.isRemote) {
      nbt.putBoolean("PowerBetaSwimPose",power$swim.compact && player.health > 0);
      nbt.putBoolean("PowerBetaSwimCrouch",power$swim.crouched && player.health > 0);
    }
  }

  @Inject(method="readtNbt",at=@At("TAIL"))
  private void power$loadPose(NbtCompound nbt,CallbackInfo ci) {
    if (!((Object)this instanceof ClientPlayerEntity player) || player.world.isRemote) return;
    power$swim.swimming = false;
    power$swim.waterDepth = 0;
    power$swim.waterJumpTicks = 0;
    if(player.health<=0) {
      power$swim.compact=power$swim.crouched=false;
      power$swim.previousAmount=power$swim.amount=0;
      power$swim.resetCamera(1.62f);
      return;
    }
    power$swim.compact = nbt.getBoolean("PowerBetaSwimPose");
    power$swim.crouched = !power$swim.compact && nbt.getBoolean("PowerBetaSwimCrouch");
    power$swim.previousAmount = power$swim.amount = power$swim.compact ? 1 : 0;
    player.spacingY = power$swim.compact ? SwimState.HEIGHT : power$swim.crouched ? 1.5f : 1.8f;
    player.field_1631 = power$swim.compact ? SwimState.EYE_HEIGHT : power$swim.crouched ? 1.27f : 1.62f;
    power$swim.resetCamera(player.field_1631);
    power$swim.deferCameraReset();
    // Entity.read has already read native Pos and built the upright box.
    if (power$swim.compact || power$swim.crouched) {
      player.y -= 1.62f - player.field_1631;
      player.prevY = player.y;
      player.field_1638 = player.y;
    }
    player.field_1640 = 0;
    player.method_1340(player.x,player.y,player.z);
  }
}
