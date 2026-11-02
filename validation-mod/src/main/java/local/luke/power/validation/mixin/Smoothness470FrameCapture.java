package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
@Mixin(value=class_555.class,priority=2300)
public class Smoothness470FrameCapture {
  @Shadow private Minecraft field_2349;
  @WrapMethod(method="method_1844") private void complete(float delta,Operation<Void> original){
    long start=Smoothness470Checks.start();try{original.call(delta);}finally{Smoothness470Checks.renderEnded(start);}
  }
  @WrapMethod(method="method_1841") private void measure(float delta,long deadline,Operation<Void> original){Smoothness470Checks.beforeFrame(field_2349);try{original.call(delta,deadline);}finally{Smoothness470Checks.afterFrame();}}
}
