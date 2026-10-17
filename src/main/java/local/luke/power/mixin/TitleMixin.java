package local.luke.power.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TitleScreen.class, priority = 800)
public class TitleMixin extends Screen {
  @Inject(method = "render", at = @At("TAIL"))
  private void power$titleError(int x, int y, float delta, CallbackInfo ci) {
    String error = local.luke.power.ui.TitleSelection.error();
    if (!error.isEmpty()) drawCenteredTextWithShadow(textRenderer, error, width / 2, 108, 0xff8888);
  }

  @Inject(method = "init", at = @At("TAIL"))
  private void power$labels(CallbackInfo ci) {
    for (Object object : buttons) {
      ButtonWidget button = (ButtonWidget) object;
      if (button.id == 3) button.text = "Texture packs";
    }
  }
}
