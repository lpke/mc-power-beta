package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fog-only material view, shared by Beta and StationAPI without changing water physics. */
@Mixin(Entity.class)
public class FullBrightFogMixin {
  @Inject(method = "isInFluid", at = @At("HEAD"), cancellable = true)
  private void power$waterFog(Material material, CallbackInfoReturnable<Boolean> cir) {
    if (material == Material.WATER && RenderControls.clearWaterFog()) cir.setReturnValue(false);
  }
}
