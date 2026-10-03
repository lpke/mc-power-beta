package paulevs.bhcreative.mixin.client;

import local.luke.creative.Modes;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
  @Inject(method = "getCanSuffocate", at = @At("HEAD"), cancellable = true)
  private void lpke$suffocate(int x, int y, int z, CallbackInfoReturnable<Boolean> ci) {
    if (Modes.protectedPlayer(this)) ci.setReturnValue(false);
  }

  @Inject(method = "isChild", at = @At("HEAD"), cancellable = true)
  private void lpke$flightCrouch(CallbackInfoReturnable<Boolean> ci) {
    if (Modes.flying((AbstractClientPlayer) (Object) this)) ci.setReturnValue(false);
  }
}
