package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.Bindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class NativeBindingMixin {
  @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;code:I", opcode = 180))
  private int power$eventCode(KeyBinding binding, Operation<Integer> original) {
    return Bindings.eventCode(binding);
  }
}
