package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.FramePresentation;
import local.luke.power.validation.FramePresentation470Checks;
import org.spongepowered.asm.mixin.Mixin;

/** Legacy comparison is confined to the disposable validation mod. */
@Mixin(value=FramePresentation.class,remap=false)
public class FramePresentation470Capture {
  @WrapMethod(method="beginFrame") private static void begin(Operation<Void> original){
    FramePresentation470Checks.beginFrame();
    if(FramePresentation470Checks.legacyMode())FramePresentation.abortFrame();else original.call();
  }
  @WrapMethod(method="deferSwap") private static boolean requested(Operation<Boolean> original){
    boolean deferred=original.call();FramePresentation470Checks.swapRequested(deferred);return deferred;
  }
  @WrapMethod(method="finishFrame") private static void end(Operation<Void> original){
    FramePresentation470Checks.beginFlush();
    try{original.call();}catch(RuntimeException|Error failure){FramePresentation470Checks.updateFailed();throw failure;}
    finally{FramePresentation470Checks.endFrame();}
  }
}
