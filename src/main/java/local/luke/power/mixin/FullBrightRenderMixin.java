package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.class_555.class)
public class FullBrightRenderMixin {
  @WrapMethod(method = "method_1841")
  private void power$render(float delta, long end, Operation<Void> original) {
    RenderControls.beginRender();
    try { original.call(delta, end); }
    finally { RenderControls.endRender(); }
  }
}
