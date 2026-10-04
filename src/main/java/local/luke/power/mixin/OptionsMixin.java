package local.luke.power.mixin;

import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Minecraft.class)
public class OptionsMixin {
  @Inject(method = "method_2135", at = @At("HEAD"), cancellable = true)
  private void power$directOptions(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    Minecraft mc = (Minecraft) (Object) this;
    if (mc.currentScreen == null && mc.world != null && local.luke.power.ui.MenuPreferences.pauseToOptions()) {
      mc.setScreen(new PowerOptionsScreen(null, true)); ci.cancel();
    }
  }
  @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
  private Screen power$options(Screen screen) {
    return screen instanceof OptionsScreen
        ? new PowerOptionsScreen(((OptionsAccessor) screen).power$parent())
        : screen;
  }
}
