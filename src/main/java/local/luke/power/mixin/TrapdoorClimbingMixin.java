package local.luke.power.mixin;

import local.luke.power.gameplay.TrapdoorClimbing;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class TrapdoorClimbingMixin {
  @Inject(method = "method_932", at = @At("RETURN"), cancellable = true)
  private void power$trapdoorLadder(CallbackInfoReturnable<Boolean> ci) {
    var entity = (LivingEntity)(Object)this;
    if (ci.getReturnValueZ() || !TrapdoorClimbing.enabled(entity)) return;
    int x = MathHelper.floot(entity.x), y = MathHelper.floot(entity.boundingBox.minY), z = MathHelper.floot(entity.z);
    if (TrapdoorClimbing.ladder(entity.world, x, y, z)) ci.setReturnValue(true);
  }
}
