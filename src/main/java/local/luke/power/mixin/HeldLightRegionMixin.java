package local.luke.power.mixin;

import local.luke.power.render.HeldLightShader;
import net.mine_diver.smoothbeta.client.render.RenderRegion;
import net.mine_diver.smoothbeta.client.render.Shaders;
import net.mine_diver.smoothbeta.mixin.client.multidraw.RenderListAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRegion.class, remap = false)
public class HeldLightRegionMixin {
  @Inject(method = "method_1909", at = @At(value = "INVOKE",
      target = "Lnet/mine_diver/smoothbeta/client/render/VboPool;drawAll()V"))
  private void power$source(CallbackInfo ci) {
    var region = (RenderListAccessor)(Object)this;
    ((HeldLightShader)Shaders.getTerrainShader()).power$heldRegion(
        region.smoothbeta_getX(), region.smoothbeta_getY(), region.smoothbeta_getZ());
  }
}
