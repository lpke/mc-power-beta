package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=Minecraft.class,priority=2400)
public class Smoothness470UseCapture {
  @WrapMethod(method="method_2107") private void measure(int button,Operation<Void> original){long start=button==1?Smoothness470Checks.start():0;try{original.call(button);}finally{Smoothness470Checks.measured(Smoothness470Checks.FULL_USE,start);}}
}
