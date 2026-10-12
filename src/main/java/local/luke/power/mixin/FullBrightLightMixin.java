package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Float shading only, inside the renderer. Simulation light levels never change. */
@Mixin({net.minecraft.world.World.class, net.minecraft.class_42.class})
public class FullBrightLightMixin {
  @Inject(method = "method_1782", at = @At("HEAD"), cancellable = true)
  private void power$light(int x, int y, int z, CallbackInfoReturnable<Float> cir) {
    if (RenderControls.renderingFullBright()) cir.setReturnValue(1f);
  }
  @Inject(method = "method_1784", at = @At("HEAD"), cancellable = true)
  private void power$blockLight(int x, int y, int z, int emission, CallbackInfoReturnable<Float> cir) {
    if (!RenderControls.renderingFullBright()) return;
    // Smooth lighting samples inside neighbouring opaque blocks for corner shadows.
    var view = (net.minecraft.world.BlockView)this;
    int id = view.getBlockId(x, y, z);
    cir.setReturnValue(id != 0 && net.minecraft.block.Block.field_1939[id] && emission == 0 ? .2f : 1f);
  }
}
