package local.luke.power.debug.mixin;

import local.luke.power.debug.*;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.input.DebugInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class DebugNativeKeysMixin {
  @ModifyExpressionValue(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Keyboard;getEventKey()I", remap=false))
  private int power$debugKey(int key) { return DebugInput.suppressNative(key) ? 0 : key; }
  @ModifyExpressionValue(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Mouse;getEventButton()I", remap=false))
  private int power$debugButton(int button) { return button >= 0 && DebugInput.suppressNative(button-100) ? -1 : button; }
}

