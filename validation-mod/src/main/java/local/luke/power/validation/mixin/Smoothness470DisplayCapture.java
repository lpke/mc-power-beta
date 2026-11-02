package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import local.luke.power.validation.FramePresentation470Checks;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;

/** Observe the real LWJGL swap entry/return rather than an earlier update-call site. */
@Mixin(value=Display.class,remap=false,priority=500)
public class Smoothness470DisplayCapture {
  @WrapMethod(method="swapBuffers") private static void swap(Operation<Void> original){
    FramePresentation470Checks.actualSwap();
    long start=Smoothness470Checks.beforeSwap();try{original.call();}finally{Smoothness470Checks.afterSwap(start);}
  }
  @WrapMethod(method="processMessages") private static void poll(Operation<Void> original){
    long start=Smoothness470Checks.start();try{original.call();}finally{Smoothness470Checks.measured(Smoothness470Checks.POLL,start);}
  }
  @WrapMethod(method="sync") private static void sync(int fps,Operation<Void> original){
    long start=Smoothness470Checks.start();try{original.call(fps);}finally{Smoothness470Checks.measured(Smoothness470Checks.SYNC,start);}
  }
}
