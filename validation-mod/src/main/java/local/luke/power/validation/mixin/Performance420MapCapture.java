package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.map.Maps;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Maps.class, remap = false)
public class Performance420MapCapture {
  @WrapMethod(method = "tick")
  private static void power$maps(Minecraft mc, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc); }
    finally { Performance420Checks.measured("mapsTick", start); }
  }
}
