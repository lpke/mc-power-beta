package local.luke.power.creative.inventory.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
  @Shadow private Minecraft minecraft;

  @Inject(method = "applyBobbing", at = @At("HEAD"), cancellable = true)
  private void creative_cancelBobbing(float delta, CallbackInfo info) {
    if (minecraft.viewEntity instanceof PlayerEntity player) {
      if (local.luke.power.creative.Modes.flying(player)) {
        info.cancel();
      }
    }
  }
}
