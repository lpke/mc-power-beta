package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=WorldRenderer.class,priority=2300)
public class Smoothness470UpdatesCapture {
  @WrapMethod(method="method_1149") private void point(int x,int y,int z,Operation<Void> original){long start=Smoothness470Checks.start();try{original.call(x,y,z);}finally{Smoothness470Checks.measured(Smoothness470Checks.NOTIFY,start);}}
  @WrapMethod(method="method_1150") private void area(int x,int y,int z,int maxX,int maxY,int maxZ,Operation<Void> original){long start=Smoothness470Checks.start();try{original.call(x,y,z,maxX,maxY,maxZ);}finally{Smoothness470Checks.measured(Smoothness470Checks.NOTIFY,start);}}
}
