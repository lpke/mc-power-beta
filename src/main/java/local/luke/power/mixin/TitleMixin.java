package local.luke.power.mixin;

import java.util.IdentityHashMap;
import java.util.Map;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioSettings.MenuControlsPosition;
import local.luke.power.ui.MainMenuMusic;
import local.luke.power.ui.MenuMusicLayout;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TitleScreen.class, priority = 800)
public class TitleMixin extends Screen {
  @Unique private final MainMenuMusic power$music = new MainMenuMusic();
  @Unique private final Map<ButtonWidget, Integer> power$buttonY = new IdentityHashMap<>();

  @Inject(method = "init", at = @At("TAIL"))
  private void power$labels(CallbackInfo ci) {
    power$music.init(minecraft, width, height);
    power$buttonY.clear();
    for (Object object : buttons) {
      ButtonWidget button = (ButtonWidget) object;
      power$buttonY.put(button, button.y);
      if (button.id == 3) button.text = "Texture packs";
    }
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void power$musicSpace(int x, int y, float delta, CallbackInfo ci) {
    int top = height, bottom = 0;
    for (Object object : buttons) {
      ButtonWidget button = (ButtonWidget) object;
      button.y = power$buttonY.computeIfAbsent(button, ignored -> button.y);
      if (button.visible) {
        top = Math.min(top, button.y);
        bottom = Math.max(bottom, button.y + 20);
      }
    }
    var settings = AudioConfig.current();
    var anchor = settings.menuControlsPosition;
    if (settings.menuControls
        && (anchor == MenuControlsPosition.MENU_BOTTOM || anchor.name().startsWith("BOTTOM_"))) {
      var panel =
          MenuMusicLayout.at(
              width,
              height,
              top,
              bottom,
              anchor,
              settings.menuControlsOffsetX,
              settings.menuControlsOffsetY);
      if (panel.x() < width / 2 + 100
          && panel.x() + panel.width() > width / 2 - 100
          && panel.y() < bottom + 8
          && panel.y() + panel.height() > top - 8) {
        int shift = Math.min(Math.max(0, top - 78), Math.max(0, bottom + 8 - panel.y()));
        for (Object object : buttons) ((ButtonWidget) object).y -= shift;
      }
    }
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void power$music(int x, int y, float delta, CallbackInfo ci) {
    int top = height, bottom = 0;
    for (Object object : buttons) {
      ButtonWidget button = (ButtonWidget) object;
      if (button.visible) {
        top = Math.min(top, button.y);
        bottom = Math.max(bottom, button.y + 20);
      }
    }
    power$music.layout(top, bottom);
    power$music.render(x, y, delta);
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    if (!power$music.press(this, x, y, button)) super.mouseClicked(x, y, button);
  }

  @Override
  public void removed() {
    power$music.removed();
    super.removed();
  }
}
