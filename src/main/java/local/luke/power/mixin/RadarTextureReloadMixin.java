package local.luke.power.mixin;

import local.luke.power.map.RadarIcons;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public final class RadarTextureReloadMixin {
  @Inject(method = "method_1096", at = @At("TAIL"))
  private void power$radarTextures(CallbackInfo ci) { RadarIcons.invalidate(); }
}
