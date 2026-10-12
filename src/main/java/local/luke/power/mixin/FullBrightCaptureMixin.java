package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.RenderControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Isometric captures draw the world outside Minecraft's normal render entry point. */
@Mixin(targets = "local.luke.power.capture.IsometricScreenshotRenderer", remap = false)
@Pseudo
public class FullBrightCaptureMixin {
  @WrapMethod(method = "doRender")
  private void power$capture(Operation<Void> original) {
    RenderControls.beginRender();
    try { original.call(); }
    finally { RenderControls.endRender(); }
  }
}
