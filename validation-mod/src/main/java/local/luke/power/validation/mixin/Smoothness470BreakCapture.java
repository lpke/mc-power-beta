package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.SingleplayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=SingleplayerInteractionManager.class,priority=2300)
public class Smoothness470BreakCapture {
  @WrapMethod(method="method_1716") private boolean measure(int x,int y,int z,int face,Operation<Boolean> original){long start=Smoothness470Checks.start();try{return original.call(x,y,z,face);}finally{Smoothness470Checks.measured(Smoothness470Checks.BREAK,start);}}
}
