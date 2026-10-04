package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.video.VideoConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.render.entity.EntityRenderer.class)
public class EntityQualityMixin {
  @ModifyExpressionValue(
      method = "method_2032",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$shadows0(boolean original) {
    return VideoConfig.current().shadows;
  }
}
