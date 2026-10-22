package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = local.luke.power.map.MapCache.class, remap = false)
public class Performance421MapCacheCapture {
  @WrapMethod(method = "tick")
  private void power$measure(Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(); }
    finally { Performance420Checks.measured("mapCacheTick", start); }
  }
}
