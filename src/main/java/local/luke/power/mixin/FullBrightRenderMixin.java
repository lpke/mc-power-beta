package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(net.minecraft.class_555.class)
public class FullBrightRenderMixin {
  @Shadow private boolean field_2330;

  // Enclose StationAPI's fog hooks too, so colour and distance use the same material.
  @WrapMethod(method = "method_1842")
  private void power$fogDistance(int pass, float delta, Operation<Void> original) {
    boolean clearWater = RenderControls.waterFogScale() == 0 && !field_2330;
    if (clearWater) RenderControls.beginFog();
    try { original.call(pass, delta); }
    finally { if (clearWater) RenderControls.endFog(); }
  }

  @WrapMethod(method = "method_1852")
  private void power$fogColour(float delta, Operation<Void> original) {
    boolean clearWater = RenderControls.waterFogScale() == 0 && !field_2330;
    if (clearWater) RenderControls.beginFog();
    try { original.call(delta); }
    finally { if (clearWater) RenderControls.endFog(); }
  }

  // The first 0.1 is cloud fog; only the second belongs to water.
  @ModifyConstant(method = "method_1842", constant = @Constant(floatValue = 0.1F, ordinal = 1))
  private float power$waterDensity(float density) {
    return density * RenderControls.waterFogScale();
  }

  @WrapMethod(method = "method_1841")
  private void power$render(float delta, long end, Operation<Void> original) {
    RenderControls.beginRender();
    try { original.call(delta, end); }
    finally { RenderControls.endRender(); }
  }
}
