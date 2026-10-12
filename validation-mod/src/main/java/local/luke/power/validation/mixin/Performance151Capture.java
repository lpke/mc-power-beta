package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Release151SphereChecks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class Performance151Capture {
  @WrapMethod(method = "method_227")
  private void power$time(Operation<Void> original) {
    if (!Release151SphereChecks.measuring()) { original.call(); return; }
    long start = System.nanoTime(); original.call(); Release151SphereChecks.tick((System.nanoTime() - start) / 1e6);
  }
}
