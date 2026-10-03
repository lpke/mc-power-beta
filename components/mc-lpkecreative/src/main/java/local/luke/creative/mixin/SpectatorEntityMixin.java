package local.luke.creative.mixin;

import local.luke.creative.Modes;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class SpectatorEntityMixin {
  @Inject(method = "addPassenger", at = @At("HEAD"), cancellable = true)
  private void lpke$push(Entity other, CallbackInfo ci) {
    if (Modes.spectator(this) || Modes.spectator(other)) ci.cancel();
  }
}
