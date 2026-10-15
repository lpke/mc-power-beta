package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import local.luke.power.render.WaterLighting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Float shading only, inside the renderer. Simulation light levels never change. */
@Mixin({net.minecraft.world.World.class, net.minecraft.class_42.class})
public class FullBrightLightMixin {
  @ModifyReturnValue(method = "method_1782", at = @At("RETURN"))
  private float power$waterLight(float shade, int x, int y, int z) {
    return RenderControls.renderingFullBright() ? shade : WaterLighting.shade(power$lightWorld(), x, y, z, shade);
  }
  @ModifyReturnValue(method = "method_1784", at = @At("RETURN"))
  private float power$waterBlockLight(float shade, int x, int y, int z, int emission) {
    return RenderControls.renderingFullBright() ? shade : WaterLighting.shade(power$lightWorld(), x, y, z, shade);
  }
  @org.spongepowered.asm.mixin.Unique
  private net.minecraft.world.World power$lightWorld() {
    return (Object)this instanceof net.minecraft.world.World world ? world
        : ((RenderBlockViewAccessor)this).power$world();
  }
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
