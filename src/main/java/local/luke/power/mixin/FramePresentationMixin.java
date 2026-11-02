package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.render.FramePresentation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Minecraft.class,priority=1800)
public class FramePresentationMixin {
  @Inject(method="run",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/util/math/Box;resetCacheCount()V",remap=true))
  private void power$begin(CallbackInfo ci){FramePresentation.beginFrame();}
  @WrapOperation(method="run",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/opengl/Display;update()V",remap=false))
  private void power$nativeUpdate(Operation<Void> original){
    FramePresentation.beginUpdate();
    try{original.call();}catch(RuntimeException|Error failure){FramePresentation.abortFrame();throw failure;}
    finally{FramePresentation.endUpdate();}
  }
  @Inject(method="run",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;logGlError(Ljava/lang/String;)V",remap=true,ordinal=1,shift=At.Shift.AFTER))
  private void power$present(CallbackInfo ci){FramePresentation.finishFrame();}
  @Inject(method="run",remap=false,at=@At("RETURN"))
  private void power$stop(CallbackInfo ci){FramePresentation.abortFrame();}
}
