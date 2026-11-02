package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.FramePresentation;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=Display.class,remap=false,priority=2000)
public class FrameSwapMixin {
  @WrapMethod(method="swapBuffers") private static void power$swap(Operation<Void> original){
    if(!FramePresentation.deferSwap())original.call();
  }
}
