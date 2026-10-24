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
    return ClientRuntime.local(minecraft) && (minecraft.player.creative_isCreative() || Modes.spectator(minecraft.player))
        ? (float) Math.max(distance, Reach.entity())
        : distance;
  }

  @Inject(method = "updateHit", at = @At("RETURN"))
  private void power$blockLimit(float delta, CallbackInfo ci) {
    var hit = minecraft.hitResult;
    if (ClientRuntime.local(minecraft)
        && (minecraft.player.creative_isCreative() || Modes.spectator(minecraft.player))
        && hit != null
        && hit.type == net.minecraft.util.hit.HitType.BLOCK) {
      var origin = minecraft.viewEntity.getInterpolatedPosition(delta);
      double eyeY = origin.y + local.luke.power.input.InteractionEye.offset(minecraft.viewEntity, delta, 0);
      double reach = minecraft.interactionManager.getBlockReachDistance();
      double dx = hit.pos.x - origin.x, dy = hit.pos.y - eyeY, dz = hit.pos.z - origin.z;
      if (dx * dx + dy * dy + dz * dz > reach * reach) minecraft.hitResult = null;
    }
  }
}
