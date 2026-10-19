package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.gameplay.ModernSwimming;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_555.class)
public class SwimmingCameraMixin {
  @Shadow private Minecraft field_2349;

  @ModifyExpressionValue(method="method_1851",at=@At(value="FIELD",
      target="Lnet/minecraft/entity/LivingEntity;field_1631:F"))
  private float power$eyeTransition(float offset,float delta) {
    return ModernSwimming.cameraOffset(field_2349.field_2807,delta,offset);
  }
}
