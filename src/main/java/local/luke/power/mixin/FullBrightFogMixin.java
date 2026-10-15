package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import local.luke.power.render.CameraEnvironment;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Beta and StationAPI share the rendered camera's material, without changing physics. */
@Mixin(Entity.class)
public class FullBrightFogMixin {
  @Inject(method = "isInFluid", at = @At("HEAD"), cancellable = true)
  private void power$waterFog(Material material, CallbackInfoReturnable<Boolean> cir) {
    if (RenderControls.fogging() && (material == Material.WATER || material == Material.LAVA))
      cir.setReturnValue((material != Material.WATER || !RenderControls.clearWaterFog())
          && CameraEnvironment.inFluid((Entity)(Object)this, material));
  }
}
