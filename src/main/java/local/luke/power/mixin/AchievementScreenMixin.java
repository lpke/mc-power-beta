package local.luke.power.mixin;

import net.minecraft.class_609;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Grow the native scrollable map and tile its texture borders without stretching icons. */
@Mixin(class_609.class)
public abstract class AchievementScreenMixin extends Screen {
  @Shadow protected int field_2618;
  @Shadow protected int field_2619;
  @Inject(method = "init", at = @At("HEAD"))
  private void power$size(CallbackInfo ci) {
    field_2618 = Math.max(256, Math.min(560, width - 32));
    field_2619 = Math.max(202, Math.min(420, height - 38));
  }
  @Inject(method = "init", at = @At("TAIL"))
  private void power$done(CallbackInfo ci) {
    for (Object object : buttons) if (object instanceof ButtonWidget button && button.id == 1) {
      button.x = (width + field_2618) / 2 - 104;
      button.y = (height + field_2619) / 2 - 26;
    }
  }
  @ModifyConstant(method = {"render", "method_1998"}, constant = @Constant(intValue = 224))
  private int power$mapWidth(int old) { return field_2618 - 32; }
  @ModifyConstant(method = {"render", "method_1998"}, constant = @Constant(intValue = 155))
  private int power$mapHeight(int old) { return field_2619 - 47; }
  @Redirect(method = "method_1998", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_609;drawTexture(IIIIII)V", ordinal = 3))
  private void power$frame(class_609 screen, int x, int y, int u, int v, int w, int h) {
    if (u != 0 || v != 0 || w != field_2618 || h != field_2619) { drawTexture(x, y, u, v, w, h); return; }
    int[] xs = {0, 16, w - 16, w}, ys = {0, 17, h - 30, h};
    int[] us = {0, 16, 240}, vs = {0, 17, 172};
    for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
      if (row == 1 && col == 1) continue;
      for (int dy = ys[row]; dy < ys[row + 1]; ) {
        int sh = Math.min(ys[row + 1] - dy, row == 1 ? 155 : row == 0 ? 17 : 30);
        for (int dx = xs[col]; dx < xs[col + 1]; ) {
          int sw = Math.min(xs[col + 1] - dx, col == 1 ? 224 : 16);
          drawTexture(x + dx, y + dy, us[col], vs[row], sw, sh); dx += sw;
        }
        dy += sh;
      }
    }
  }
}
