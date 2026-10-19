package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.validation.CreativePressChecks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class CreativePressCapture {
  @WrapOperation(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/Minecraft;method_2107(I)V", ordinal = 0))
  private void power$physicalPress(Minecraft mc, int button, Operation<Void> original) {
    original.call(mc, button);
    CreativePressChecks.nativePress(mc, button);
  }
}
