package local.luke.power.mixin;

import local.luke.power.input.DetachedCamera;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public class FreecamBlockEntityDistanceMixin {
  @Redirect(method = "method_1278", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/block/entity/BlockEntity;method_1074(DDD)D"))
  private double power$blockEntityDistance(BlockEntity entity, double x, double y, double z) {
    return DetachedCamera.isActive() && !local.luke.power.render.RenderControls.photoCamera()
        ? entity.method_1074(DetachedCamera.x(), DetachedCamera.y(), DetachedCamera.z())
        : entity.method_1074(x, y, z);
  }
}
