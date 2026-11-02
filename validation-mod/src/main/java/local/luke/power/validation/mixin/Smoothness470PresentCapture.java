package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.validation.FramePresentation470Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class Smoothness470PresentCapture {
  @WrapOperation(method="run",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/opengl/Display;update()V",remap=false))
  private void measure(Operation<Void> original){
    FramePresentation470Checks.beginUpdate();
    try{original.call();}catch(RuntimeException|Error failure){FramePresentation470Checks.updateFailed();throw failure;}
    finally{FramePresentation470Checks.endUpdate();}
  }
  @Inject(method="run",remap=false,at=@At("RETURN"))
  private void stop(CallbackInfo ci){FramePresentation470Checks.restore();}
}
