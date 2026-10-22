package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native swap-call cadence, separate from the world-render start after each tick. */
@Mixin(Minecraft.class)
public class Performance421PresentCapture {
  @WrapOperation(method = "run", remap = false,
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false))
  private void power$present(Operation<Void> original) {
    long start = Performance420Checks.beforePresent();
    try { original.call(); }
    finally { Performance420Checks.measured("displayUpdate", start); }
  }
}
