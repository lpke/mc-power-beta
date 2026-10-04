package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.video.VideoConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.render.WorldRenderer.class)
public class WorldQualityMixin {
  @ModifyExpressionValue(
      method = "method_1537",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$leaves0(boolean original) {
    return VideoConfig.current().leaves;
  }

  @ModifyExpressionValue(
      method = "method_1552",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$clouds1(boolean original) {
    return VideoConfig.current().clouds;
  }
}
