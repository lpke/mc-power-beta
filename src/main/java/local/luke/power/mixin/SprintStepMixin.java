package local.luke.power.mixin;

import local.luke.power.sprint.SprintController;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public class SprintStepMixin {
  @ModifyConstant(method = "move", constant = @Constant(floatValue = .05f))
  private float power$sprintStepDelay(float threshold) {
    Entity entity = (Entity) (Object) this;
    // Beta waits for visual step smoothing before stepping again. Sprint reaches
    // the next half-step sooner, so retain the camera offset without its delay.
    return SprintController.active(entity) && !entity.method_1334()
        ? Float.POSITIVE_INFINITY : threshold;
  }
}
