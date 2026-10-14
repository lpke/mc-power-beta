package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.input.DetachedCamera;
import local.luke.power.input.PlayerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class CameraPerspectiveMixin {
  @WrapOperation(method = "run", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
      target = "Lnet/minecraft/client/option/GameOptions;thirdPerson:Z"))
  private void power$keepPerspective(GameOptions options, boolean value, Operation<Void> original) {
    Minecraft mc = (Minecraft)(Object)this;
    // Beta forces first person each frame when the player's eyes are inside a solid block.
    // Detached and spectator cameras retain the perspective chosen by the player.
    if (DetachedCamera.isActive() || mc.player instanceof PlayerMode mode && mode.power$spectator()) return;
    original.call(options, value);
  }
}
