package local.luke.power.mixin;

import local.luke.power.audio.AudioController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class UnderwaterSoundMixin {
  @Inject(method = "baseTick", at = @At("TAIL"))
  private void power$underwater(CallbackInfo ci) {
    if ((Object)this instanceof ClientPlayerEntity player) AudioController.waterSample(player);
  }
}
