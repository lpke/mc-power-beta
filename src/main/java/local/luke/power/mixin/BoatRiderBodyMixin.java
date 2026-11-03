package local.luke.power.mixin;

import net.minecraft.class_113;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class BoatRiderBodyMixin {
  @ModifyVariable(method = "tick", at = @At("LOAD"), index = 6)
  private float power$boatBodyTarget(float target) {
    LivingEntity rider = (LivingEntity) (Object) this;
    if (rider.field_1595 instanceof class_113 && !rider.world.isRemote) {
      // A seated rider has no walking heading. The native idle target holds the
      // torso still until a 50-degree head turn kicks it forward by about ten
      // degrees. Ease toward the view each tick instead, through the same native
      // smoothing and head limits used for walking.
      return rider.yaw;
    }
    return target;
  }
}
