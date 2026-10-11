package local.luke.power.mixin;

import local.luke.power.input.DetachedCamera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class FreecamEntityDistanceMixin {
  @ModifyVariable(method = "method_1544", at = @At("HEAD"), argsOnly = true)
  private Vec3d power$entityDistanceOrigin(Vec3d playerView) {
    // Change only the render-distance reference. Entity simulation and renderer
    // translations still use the real player and the existing view transform.
    return DetachedCamera.isActive()
        ? Vec3d.createCached(DetachedCamera.x(), DetachedCamera.y(), DetachedCamera.z())
        : playerView;
  }
}
