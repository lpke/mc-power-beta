package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Smoothness470Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=Minecraft.class,priority=2500)
public class Smoothness470TickCapture {
  @WrapMethod(method="tick") private void tick(Operation<Void> original){
    long start=Smoothness470Checks.start();Smoothness470Checks.tickStarted(start);
    try{original.call();}finally{Smoothness470Checks.measured(Smoothness470Checks.CLIENT_TICK,start);}
  }
}
