package local.luke.power.debug.mixin;

import local.luke.power.debug.*;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.input.DebugInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class DebugNativeKeysMixin {
  @WrapOperation(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Keyboard;next()Z", remap=false))
  private boolean power$keyboardEvent(Operation<Boolean> original) {
    boolean next=original.call();
    if(next) DebugKeys.event(org.lwjgl.input.Keyboard.getEventKey(),org.lwjgl.input.Keyboard.getEventKeyState(),org.lwjgl.input.Keyboard.isRepeatEvent());
    return next;
  }
  @WrapOperation(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Mouse;next()Z", remap=false))
  private boolean power$mouseEvent(Operation<Boolean> original) {
    boolean next=original.call();
    if(next && org.lwjgl.input.Mouse.getEventButton()>=0)
      DebugKeys.event(org.lwjgl.input.Mouse.getEventButton()-100,org.lwjgl.input.Mouse.getEventButtonState(),false);
    return next;
  }
  @ModifyExpressionValue(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Keyboard;getEventKey()I", remap=false))
  private int power$debugKey(int key) { return DebugInput.suppressNative(key) ? 0 : key; }
  @ModifyExpressionValue(method="tick", at=@At(value="INVOKE", target="Lorg/lwjgl/input/Mouse;getEventButton()I", remap=false))
  private int power$debugButton(int button) { return button >= 0 && DebugInput.suppressNative(button-100) ? -1 : button; }
}

