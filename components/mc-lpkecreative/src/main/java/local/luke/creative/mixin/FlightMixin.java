package local.luke.creative.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.creative.*;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class FlightMixin extends net.minecraft.entity.Entity {
  public FlightMixin(net.minecraft.level.Level level) {
    super(level);
  }

  @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
  private void lpke$travel(float side, float forward, CallbackInfo ci) {
    if ((Object) this instanceof PlayerEntity player && FlightController.travel(player)) {
      fallDistance = 0;
      ci.cancel();
    }
  }

  @ModifyExpressionValue(
      method = "processMovement",
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/entity/living/LivingEntity;jumping:Z",
              opcode = org.objectweb.asm.Opcodes.GETFIELD))
  private boolean lpke$jump(boolean value) {
    return value && !((Object) this instanceof PlayerEntity player && Modes.flying(player));
  }

  @Inject(method = "isRideable", at = @At("HEAD"), cancellable = true)
  private void lpke$push(CallbackInfoReturnable<Boolean> ci) {
    if (Modes.spectator(this)) ci.setReturnValue(false);
  }
}
