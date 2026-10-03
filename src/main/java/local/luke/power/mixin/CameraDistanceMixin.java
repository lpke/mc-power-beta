package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_555;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_555.class)
public class CameraDistanceMixin {
  @ModifyExpressionValue(method = "method_1851", at = @At(value = "FIELD", target = "Lnet/minecraft/class_555;field_2359:F"))
  private float power$distance(float original) { return original * (float)(VisualConfig.current().thirdPersonDistance / 4); }
  @ModifyExpressionValue(method = "method_1851", at = @At(value = "FIELD", target = "Lnet/minecraft/class_555;field_2360:F"))
  private float power$previousDistance(float original) { return original * (float)(VisualConfig.current().thirdPersonDistance / 4); }
}
