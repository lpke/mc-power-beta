package local.luke.power.mixin;

import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Minecraft.class)
public class OptionsMixin {
  @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
  private Screen power$options(Screen screen) {
    return screen instanceof OptionsScreen
        ? new PowerOptionsScreen(((OptionsAccessor) screen).power$parent())
        : screen;
  }
}
