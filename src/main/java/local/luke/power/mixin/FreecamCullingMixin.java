package local.luke.power.mixin;

import local.luke.power.input.DetachedCamera;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Apply after Entity Culling adds these methods to both base classes.
@Mixin(value = {Entity.class, BlockEntity.class}, priority = 900)
public class FreecamCullingMixin {
  @Inject(method = {"isCulled", "isOutOfCamera"}, at = @At("HEAD"), cancellable = true, remap = false)
  private void power$freecamVisible(CallbackInfoReturnable<Boolean> cir) {
    // Keep the worker's player-view results current so normal culling resumes on exit.
    if (DetachedCamera.isActive()) cir.setReturnValue(false);
  }
}
