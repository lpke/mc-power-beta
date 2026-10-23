package local.luke.power.mixin;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeSet;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioSettings.MenuControlsPosition;
import local.luke.power.ui.PauseMenuMusic;
import local.luke.power.ui.PauseMenuTools;
import net.minecraft.class_525;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {class_525.class, net.minecraft.client.gui.screen.TitleScreen.class}, priority = 800)
public class PauseMusicMixin extends Screen implements local.luke.power.input.InventoryGui {
  @Override public boolean power$containsGuiSpace(int x, int y) {
    if (power$tools.contains(x,y)) return true;
    var music=power$music.bounds();
    if (music!=null && x>=music.x() && x<music.x()+music.width() && y>=music.y() && y<music.y()+music.height()) return true;
    for (Object value : buttons) {
      var button=(ButtonWidget)value;
      var bounds=(ButtonBoundsAccessor)(Object)button;
      if (button.visible && x>=button.x && x<button.x+bounds.power$width()
          && y>=button.y && y<button.y+bounds.power$height()) return true;
    }
    return false;
  }
  @Unique private final PauseMenuMusic power$music = new PauseMenuMusic();
  @Unique private final PauseMenuTools power$tools = new PauseMenuTools();
  @Unique private final Map<ButtonWidget, Integer> power$buttonY = new IdentityHashMap<>();

  @Inject(method = "init", at = @At("TAIL"))
  private void power$init(CallbackInfo ci) {
    power$music.init(minecraft, width, height);
    power$tools.init(minecraft, width, height);
    power$tools.configure(this, buttons, (Object)this instanceof class_525);
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
    int toolsSpace = power$tools.reserveAboveBack();
    if (toolsSpace > 0 && (Object)this instanceof class_525) {
      for (Object object : buttons) {
        ButtonWidget back = (ButtonWidget)object;
        if (back.id != 4) continue;
        // Leave the default toolbar between the title and Back to game when space permits.
        int shift = Math.min(Math.max(0, 52 + toolsSpace - back.y), Math.max(0, height - 16 - bottom));
        for (Object entry : buttons) ((ButtonWidget)entry).y += shift;
        top += shift; bottom += shift;
        break;
      }
    }
    top -= toolsSpace;
    var position = AudioConfig.current().menuControlsPosition;
    boolean narrowSide = width / 2 - 112 < 180
        && (position == MenuControlsPosition.MIDDLE_LEFT || position == MenuControlsPosition.MIDDLE_RIGHT);
    if (power$music.visible()
        && (position == MenuControlsPosition.MENU_TOP || narrowSide)) {
      // At small GUI sizes, reserve room below the title instead of covering it.
      int shift = Math.max(0, Math.min(96 + (AudioConfig.current().menuControlsScrub ? 16 : 0) - top, height - 16 - bottom));
      for (Object object : buttons) ((ButtonWidget) object).y += shift;
      top += shift;
      bottom += shift;
    }
    if (power$music.visible() && position == MenuControlsPosition.MENU_BOTTOM) {
      int needed = Math.max(0, bottom + (AudioConfig.current().menuControlsScrub ? 48 : 32) + 20 - height);
      if (needed > 0 && (Object) this instanceof net.minecraft.client.gui.screen.TitleScreen) {
        // The title menu has an extra gap before Options. Use that space first.
        TreeSet<Integer> rows = new TreeSet<>();
        for (Object object : buttons) if (((ButtonWidget) object).visible) rows.add(((ButtonWidget) object).y);
        if (rows.size() > 1) {
          int last = rows.last(), previous = rows.lower(last);
          int close = Math.min(needed, Math.max(0, last - previous - 24));
          for (Object object : buttons) if (((ButtonWidget) object).y == last) ((ButtonWidget) object).y -= close;
          bottom -= close;
          needed -= close;
        }
      }
      int minimumTop = toolsSpace > 0 && (Object)this instanceof class_525 ? 52 : 48;
      int shift = Math.min(needed, Math.max(0, top - minimumTop));
      for (Object object : buttons) ((ButtonWidget) object).y -= shift;
      top -= shift;
      bottom -= shift;
    }
    power$music.layout(top, bottom);
    power$tools.layout(buttons, power$music.bounds());
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void power$space(int x, int y, float delta, CallbackInfo ci) {
    power$layout();
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void power$render(int x, int y, float delta, CallbackInfo ci) {
    power$music.render(x, y, delta);
    power$tools.render(x, y, delta);
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    power$layout();
    if (power$tools.press(x, y, button)) return;
    if (power$music.press(x, y, button)) return;
    if ((Object) this instanceof net.minecraft.client.gui.screen.TitleScreen && minecraft.world == null
        && (button == 0 || button == 1) && local.luke.power.ui.TitlePreview.hitTest.contains(x, y, width)) {
      minecraft.soundManager.method_2009("random.click", 1, 1);
      local.luke.power.ui.TitleSelection.cycle(button == 1 ? -1 : 1);
      return;
    }
    super.mouseClicked(x, y, button);
  }
}
