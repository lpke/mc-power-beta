package local.luke.power.validation.mixin;

import local.luke.power.validation.FreecamDistanceChecks;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderCapture {
  @Inject(method="method_1921",at=@At("HEAD"),cancellable=true)
  private void power$record(Entity entity,float delta,CallbackInfo ci) {
    if(FreecamDistanceChecks.recording) {FreecamDistanceChecks.entities.add(entity);ci.cancel();}
  }
}
