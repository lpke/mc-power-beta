package local.luke.power.mixin;

import local.luke.power.audio.AudioController;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class UnderwaterPlayerTickMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void power$underwater(CallbackInfo ci) {
    if ((Object)this instanceof ClientPlayerEntity player) AudioController.waterTick(player);
  }
}
