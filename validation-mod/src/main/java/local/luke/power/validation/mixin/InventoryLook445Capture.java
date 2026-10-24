package local.luke.power.validation.mixin;

import local.luke.power.validation.InventoryLook445Checks;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the pose actually submitted to the native inventory model renderer. */
@Mixin(EntityRenderDispatcher.class)
public abstract class InventoryLook445Capture {
  @Inject(method = "method_1920", at = @At("HEAD"))
  private void power$pose(Entity entity, double x, double y, double z, float yaw, float delta, CallbackInfo ci) {
    InventoryLook445Checks.model(entity);
  }
}
