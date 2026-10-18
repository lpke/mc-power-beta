package local.luke.power.mixin;

import local.luke.power.visual.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.class_113;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PlayerEntityRenderer.class)
public class CapeRotationMixin {
  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 0))
  private float power$pitch(float angle) {
    return VisualConfig.current().limitCapeRotation ? CapeAngles.pitch(angle) : angle;
  }

  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 1))
  private float power$roll(float angle, @Local(argsOnly = true) PlayerEntity player) {
    // Boat travel and seat motion must not twist the cape across the rider's back.
    if (player.field_1595 instanceof class_113) return 0;
    return VisualConfig.current().limitCapeRotation ? CapeAngles.sideways(angle) : angle;
  }

  @ModifyArg(method = "method_827", index = 0,
      slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 2))
  private float power$yaw(float angle, @Local(argsOnly = true) PlayerEntity player) {
    if (player.field_1595 instanceof class_113) return 0;
    return VisualConfig.current().limitCapeRotation ? CapeAngles.sideways(angle) : angle;
  }
}
