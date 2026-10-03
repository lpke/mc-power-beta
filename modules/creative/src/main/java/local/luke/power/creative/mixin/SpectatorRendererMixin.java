package local.luke.power.creative.mixin;

import local.luke.power.creative.Modes;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class SpectatorRendererMixin {
  @Inject(method = "method_341", at = @At("HEAD"), cancellable = true)
  private void power$hide(
      PlayerEntity player, double x, double y, double z, float yaw, float delta, CallbackInfo ci) {
    if (Modes.spectator(player)) ci.cancel();
  }
}
