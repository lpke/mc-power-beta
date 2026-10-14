package local.luke.power.mixin;

import local.luke.power.render.RenderControls;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class RenderKeysMixin {
  @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKeyState()Z", ordinal = 0, remap = false, shift = At.Shift.AFTER))
  private void power$renderKey(CallbackInfo ci) {
    if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) {
      RenderControls.press(Keyboard.getEventKey());
      local.luke.power.gameplay.MiningControls.press(Keyboard.getEventKey());
    }
  }
}
