package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.audio.*;
import local.luke.power.input.PlayerMode;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Creative discarding cancels the native player attack before its damage callback. */
@Mixin(InteractionManager.class)
public abstract class CreativeVehicleAttackSoundMixin {
  @WrapMethod(method = "attackEntity")
  private void power$discard(PlayerEntity player, Entity target, Operation<Void> original) {
    boolean watch = VehicleAttackSounds.eligible(player, target)
        && player instanceof PlayerMode mode && mode.power$creative();
    original.call(player, target);
    if (watch && target.dead) VehicleAttackSounds.hit(player);
    // Modern game-mode attack resets even when the entity rejects or short-circuits damage.
    VehicleAttackSounds.reset(player);
  }
}
