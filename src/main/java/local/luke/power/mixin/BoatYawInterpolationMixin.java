package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.class_113;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class BoatYawInterpolationMixin {
  @WrapOperation(method = "method_1921", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;method_1920(Lnet/minecraft/entity/Entity;DDDFF)V"))
  private void power$boatYaw(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z,
      float yaw, float delta, Operation<Void> original) {
    if (entity instanceof class_113 && !entity.world.isRemote) {
      // Boat.tick wraps its heading at 360 degrees. Interpolate the same short turn
      // used by the native passenger update without changing either entity's angles.
      float turn = (entity.yaw - entity.prevYaw) % 360;
      if (turn < -180) turn += 360;
      if (turn >= 180) turn -= 360;
      yaw = entity.prevYaw + turn * delta;
    }
    original.call(dispatcher, entity, x, y, z, yaw, delta);
  }
}
