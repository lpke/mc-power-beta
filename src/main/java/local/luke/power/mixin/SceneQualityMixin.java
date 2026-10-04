package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.video.VideoConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.class_555.class)
public class SceneQualityMixin {
  @ModifyExpressionValue(
      method = "method_1841",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$water0(boolean original) {
    return VideoConfig.current().water;
  }

  @ModifyExpressionValue(
      method = "method_1846",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$weather1(boolean original) {
    return VideoConfig.current().weather;
  }

  @ModifyExpressionValue(
      method = "method_1847",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$weather2(boolean original) {
    return VideoConfig.current().weather;
  }
}
