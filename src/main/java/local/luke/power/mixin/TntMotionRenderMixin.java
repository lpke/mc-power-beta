package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.render.TntMotion;
import net.minecraft.class_365;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class TntMotionRenderMixin {
  @WrapOperation(method = "method_1921", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;method_1920(Lnet/minecraft/entity/Entity;DDDFF)V"))
  private void power$smooth(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z,
      float yaw, float delta, Operation<Void> original) {
    if (entity instanceof class_365 && !entity.world.isRemote && GameplayConfig.current().limitTnt) {
      var motion = ((TntMotion.Access)entity).power$tntMotion();
      if (motion.initialized()) {
        x += motion.x(delta) - (entity.field_1637 + (entity.x - entity.field_1637) * delta);
        y += motion.y(delta) - (entity.field_1638 + (entity.y - entity.field_1638) * delta);
        z += motion.z(delta) - (entity.field_1639 + (entity.z - entity.field_1639) * delta);
      }
    }
    original.call(dispatcher, entity, x, y, z, yaw, delta);
  }
}
