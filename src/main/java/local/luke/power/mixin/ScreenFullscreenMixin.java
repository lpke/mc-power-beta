package local.luke.power.mixin;

import local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener;
import local.luke.power.input.FullscreenKey;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Screen.class)
public abstract class ScreenFullscreenMixin {
  @ModifyConstant(method = "onKeyboardEvent", constant = @Constant(intValue = 87))
  private int power$fullscreenBinding(int original) {
    if (Keyboard.isRepeatEvent()
        || (Object)this instanceof PowerOptionsScreen options && options.capturingBinding()) return -1000;
    return FullscreenKey.eventCode(KeyBindingListener.toggleFullscreen);
  }
}
