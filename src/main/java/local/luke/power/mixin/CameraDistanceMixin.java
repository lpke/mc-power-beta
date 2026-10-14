package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.input.PlayerMode;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_27;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_555.class)
public class CameraDistanceMixin {
  @Shadow private Minecraft field_2349;

  @WrapOperation(method = "method_1851", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_160(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/class_27;"))
  private class_27 power$spectatorCamera(World world, Vec3d start, Vec3d end, Operation<class_27> original) {
    if (field_2349.field_2807 instanceof PlayerMode mode && mode.power$spectator()) return null;
    return original.call(world, start, end);
  }

  @ModifyExpressionValue(method = "method_1851", at = @At(value = "FIELD", target = "Lnet/minecraft/class_555;field_2359:F"))
  private float power$distance(float original) { return original * (float)(VisualConfig.current().thirdPersonDistance / 4); }
  @ModifyExpressionValue(method = "method_1851", at = @At(value = "FIELD", target = "Lnet/minecraft/class_555;field_2360:F"))
  private float power$previousDistance(float original) { return original * (float)(VisualConfig.current().thirdPersonDistance / 4); }
}
