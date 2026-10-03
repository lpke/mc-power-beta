package local.luke.power.creative.mixin;

import local.luke.power.creative.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class RendererMixin {
  @Shadow private Minecraft minecraft;

  @Inject(method = "updateCamera", at = @At("HEAD"), cancellable = true)
  private void power$hideHand(float delta, int anaglyph, CallbackInfo ci) {
    if (Modes.spectator(minecraft.player)) ci.cancel();
  }

  @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
      method = "updateHit",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/ClientInteractionManager;getBlockReachDistance()F"))
  private float power$rayDistance(float distance) {
    return ClientRuntime.local(minecraft) && minecraft.player.creative_isCreative()
        ? (float) Math.max(distance, Reach.entity())
        : distance;
  }

  @Inject(method = "updateHit", at = @At("RETURN"))
  private void power$blockLimit(float delta, CallbackInfo ci) {
    var hit = minecraft.hitResult;
    if (ClientRuntime.local(minecraft)
        && minecraft.player.creative_isCreative()
        && hit != null
        && hit.type == net.minecraft.util.hit.HitType.BLOCK
        && minecraft.viewEntity.getInterpolatedPosition(delta).distance(hit.pos)
            > minecraft.interactionManager.getBlockReachDistance()) minecraft.hitResult = null;
  }
}
