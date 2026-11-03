package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.*;
import local.luke.power.input.PlayerMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class VehicleAttackSoundMixin implements VehicleAttackSoundPlayer {
  @Unique private int power$vehicleSoundCharge;
  @Unique private int power$vehicleSoundItem;

  public int power$vehicleSoundCharge() { return power$vehicleSoundCharge; }
  public void power$resetVehicleSoundCharge() { power$vehicleSoundCharge = 0; }

  @Inject(method = "tick", at = @At("TAIL"))
  private void power$charge(CallbackInfo ci) {
    PlayerEntity player = (PlayerEntity) (Object) this;
    if (!AudioConfig.current().vehicleAttackSounds || player.world.isRemote || player.dead
        || player instanceof PlayerMode mode && mode.power$spectator()) return;
    // Modern increments, then resets on a changed held item. Identity means the item type, not wear/count.
    if (power$vehicleSoundCharge < 25) power$vehicleSoundCharge++;
    var held = player.method_502(); int item = held == null ? 0 : held.itemId;
    if (power$vehicleSoundItem != item) {
      power$vehicleSoundItem = item;
      power$vehicleSoundCharge = 0;
    }
  }

  @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
  private boolean power$hit(Entity target, Entity attacker, int damage, Operation<Boolean> original) {
    PlayerEntity player = (PlayerEntity) (Object) this;
    boolean watch = VehicleAttackSounds.eligible(player, target);
    boolean accepted = original.call(target, attacker, damage);
    if (watch && accepted) VehicleAttackSounds.hit(player);
    return accepted;
  }

  @Inject(method = "attack", at = @At("RETURN"))
  private void power$attack(Entity target, CallbackInfo ci) {
    PlayerEntity player = (PlayerEntity) (Object) this;
    if (AudioConfig.current().vehicleAttackSounds && !player.world.isRemote) power$resetVehicleSoundCharge();
  }
}
