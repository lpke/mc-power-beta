package local.luke.power.mixin;

import local.luke.power.visual.VisualConfig;
import net.minecraft.class_556;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(class_556.class)
public abstract class LowFireMixin {
  @ModifyArg(method = "method_1867", at = @At(value = "INVOKE",
      target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", remap = false), index = 1)
  private float power$lowerFire(float y) {
    return VisualConfig.current().lowFire ? y - 0.3F : y;
  }
}
