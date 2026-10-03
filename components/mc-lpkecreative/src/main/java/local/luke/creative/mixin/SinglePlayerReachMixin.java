package local.luke.creative.mixin;

import local.luke.creative.*;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(SinglePlayerClientInteractionManager.class)
public abstract class SinglePlayerReachMixin extends ClientInteractionManager {
  public SinglePlayerReachMixin(Minecraft mc) {
    super(mc);
  }

  @Inject(
      method = {"playerDigBlock", "digBlock"},
      at = @At("HEAD"),
      cancellable = true)
  private void lpke$dig(int x, int y, int z, int face, CallbackInfo ci) {
    if (Modes.spectator(minecraft.player)) ci.cancel();
  }

  @Inject(method = "activateBlock", at = @At("HEAD"), cancellable = true)
  private void lpke$break(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> ci) {
    if (Modes.spectator(minecraft.player)) ci.setReturnValue(false);
  }
}
