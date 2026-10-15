package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.GameplayKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class DebugGraphMixin {
  @WrapOperation(method = "run", at = @At(value = "FIELD",
      target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z", opcode = 180))
  private boolean power$graph(GameOptions options, Operation<Boolean> original) {
    return original.call(options) && GameplayKeys.graph();
  }
}
