package local.luke.power.mixin;

import local.luke.power.sprint.SprintController;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class SprintInputMixin {
  @Inject(
      method = "method_937",
      at =
          @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;method_937()V"))
  private void power$sprintInput(CallbackInfo ci) {
    SprintController.movement((ClientPlayerEntity) (Object) this);
  }

  @Inject(method = "method_937", at = @At("TAIL"))
  private void power$sprintParticles(CallbackInfo ci) {
    SprintController.particles((ClientPlayerEntity) (Object) this);
  }
}
