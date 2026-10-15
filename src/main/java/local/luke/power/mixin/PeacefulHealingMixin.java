package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public class PeacefulHealingMixin {
  @WrapOperation(method = "method_937", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/player/PlayerEntity;method_939(I)V"))
  private void power$heal(PlayerEntity player, int amount, Operation<Void> original) {
    if (player.world.isRemote || WorldRules.enabled(player.world.method_262(), GameRule.PEACEFUL_HEALTH_REGENERATION))
      original.call(player, amount);
  }
}
