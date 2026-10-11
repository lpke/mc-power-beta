package local.luke.power.validation.mixin;

import local.luke.power.validation.FreecamDistanceChecks;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderCapture {
  @Inject(method="method_1277",at=@At("HEAD"),cancellable=true)
  private void power$record(BlockEntity entity,double x,double y,double z,float delta,CallbackInfo ci) {
    if(FreecamDistanceChecks.recording) {FreecamDistanceChecks.blocks.add(entity);ci.cancel();}
  }
}
