package local.luke.power.mixin;

import local.luke.power.visual.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.class_113;
import net.minecraft.class_549;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PlayerEntityRenderer.class)
public class CapeRotationMixin {
  @Shadow private BipedEntityModel bipedModel;

  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 0))
  private float power$pitch(float angle, @Local(argsOnly = true) PlayerEntity player,
      @Local(argsOnly = true) float delta) {
    if (!VisualConfig.current().limitCapeRotation) return angle;
    float limited = CapeAngles.pitch(angle);
    if (player.field_1595 != null) return limited;
    // Reserve the complete interpolated gait rather than chasing each leg. Remove
    // native walking bob as well; the cape still keeps vertical and forward drag.
    float amplitude = player.field_1048 + (player.field_1049 - player.field_1048) * delta;
    float walk = player.field_524 + (player.field_525 - player.field_524) * delta;
    float distance = player.field_1634 + (player.field_1635 - player.field_1634) * delta;
    limited = CapeAngles.pitch(angle - MathHelper.sin(distance * 6) * 32 * walk);
    float right = CapeAngles.strideClearance(amplitude, bipedModel.rightLeg.pivotY, bipedModel.rightLeg.pivotZ);
    float left = CapeAngles.strideClearance(amplitude, bipedModel.leftLeg.pivotY, bipedModel.leftLeg.pivotZ);
    return Math.max(limited, Math.max(right, left));
  }

  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 1))
  private float power$roll(float angle, @Local(argsOnly = true) PlayerEntity player) {
    // Vehicle travel and seat motion must not twist the cape across the rider's back.
    if (player.field_1595 instanceof class_113 || player.field_1595 instanceof class_549) return 0;
    return VisualConfig.current().limitCapeRotation ? CapeAngles.sideways(angle) : angle;
  }

  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 2))
  private float power$yaw(float angle, @Local(argsOnly = true) PlayerEntity player) {
    if (player.field_1595 instanceof class_113 || player.field_1595 instanceof class_549) return 0;
    return VisualConfig.current().limitCapeRotation ? CapeAngles.sideways(angle) : angle;
  }
}
