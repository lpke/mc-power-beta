package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Release151SphereChecks;
import net.minecraft.class_68;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public class Render151Capture {
  @WrapMethod(method = "method_1544")
  private void power$time(Vec3d pos, class_68 frustum, float delta, Operation<Void> original) {
    if (!Release151SphereChecks.measuring()) { original.call(pos, frustum, delta); return; }
    long start = System.nanoTime(); original.call(pos, frustum, delta); Release151SphereChecks.render((System.nanoTime() - start) / 1e6);
  }
}
