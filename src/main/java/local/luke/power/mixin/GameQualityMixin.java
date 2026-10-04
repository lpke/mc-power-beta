package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.video.VideoConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.Minecraft.class)
public class GameQualityMixin {
  @ModifyExpressionValue(
      method = "run",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private boolean power$grass0(boolean original) {
    return VideoConfig.current().grass;
  }

  @ModifyExpressionValue(
      method = "method_2147",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
  private static boolean power$vignette1(boolean original) {
    return true;
  }
}
