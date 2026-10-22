package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = local.luke.power.map.Minimap.class, remap = false)
public class Performance421MinimapCapture {
  @WrapMethod(method = "render(Lnet/minecraft/client/Minecraft;F)V")
  private static void power$measure(Minecraft mc, float delta, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(mc, delta); }
    finally { Performance420Checks.measured("minimapRender", start); }
  }
}
