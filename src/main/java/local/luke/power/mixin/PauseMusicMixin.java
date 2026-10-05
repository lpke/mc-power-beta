package local.luke.power.mixin;

import java.util.IdentityHashMap;
import java.util.Map;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioSettings.MenuControlsPosition;
import local.luke.power.ui.PauseMenuMusic;
import net.minecraft.class_525;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_525.class, priority = 800)
public class PauseMusicMixin extends Screen {
  @Unique private final PauseMenuMusic power$music = new PauseMenuMusic();
  @Unique private final Map<ButtonWidget, Integer> power$buttonY = new IdentityHashMap<>();

  @Inject(method = "init", at = @At("TAIL"))
  private void power$init(CallbackInfo ci) {
    power$music.init(minecraft, width, height);
    power$buttonY.clear();
  }

  @Unique
  private void power$layout() {
    int top = height, bottom = 0;
    for (Object object : buttons) {
      ButtonWidget button = (ButtonWidget) object;
      button.y = power$buttonY.computeIfAbsent(button, ignored -> button.y);
      if (button.visible) {
        top = Math.min(top, button.y);
        bottom = Math.max(bottom, button.y + 20);
      }
    }
    var position = AudioConfig.current().menuControlsPosition;
    boolean narrowSide = width / 2 - 112 < 180
        && (position == MenuControlsPosition.MIDDLE_LEFT || position == MenuControlsPosition.MIDDLE_RIGHT);
    if (AudioConfig.current().menuControls
        && (position == MenuControlsPosition.MENU_TOP || narrowSide)) {
      // At small GUI sizes, reserve room below the title instead of covering it.
      int shift = Math.max(0, Math.min(96 + (AudioConfig.current().menuControlsScrub ? 16 : 0) - top, height - 16 - bottom));
      for (Object object : buttons) ((ButtonWidget) object).y += shift;
      top += shift;
      bottom += shift;
    }
    power$music.layout(top, bottom);
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void power$space(int x, int y, float delta, CallbackInfo ci) {
    power$layout();
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void power$render(int x, int y, float delta, CallbackInfo ci) {
    power$music.render(x, y, delta);
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    power$layout();
    if (!power$music.press(x, y, button)) super.mouseClicked(x, y, button);
  }
}
