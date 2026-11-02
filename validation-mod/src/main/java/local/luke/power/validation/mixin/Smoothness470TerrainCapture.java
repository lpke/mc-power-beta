package local.luke.power.validation.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.*;
@Mixin(value=class_66.class,priority=2300)
public class Smoothness470TerrainCapture {
  @Shadow public boolean field_249;
  @WrapMethod(method="method_296") private void measure(Operation<Void> original){long start=field_249?Smoothness470Checks.start():0;try{original.call();}finally{Smoothness470Checks.measured(Smoothness470Checks.MESH,start);}}
}
