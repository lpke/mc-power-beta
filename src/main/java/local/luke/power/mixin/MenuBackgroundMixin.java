package local.luke.power.mixin;

import local.luke.power.ui.MenuPanorama;
import local.luke.power.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MenuBackgroundMixin extends net.minecraft.client.gui.DrawableHelper {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width;
  @Shadow public int height;
  @Inject(method = "renderBackground(I)V", at = @At("TAIL"))
  private void power$inventoryMinimap(int ignored, CallbackInfo ci) {
    local.luke.power.map.Minimap.renderInventory(minecraft, local.luke.power.map.Minimap.frameDelta());
  }
  @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true)
  private void power$panorama(int ignored, CallbackInfo ci) {
    if (!MenuPanorama.enabled(minecraft)) return;
    MenuPanorama.render(minecraft);
    if (minecraft.currentScreen instanceof UiScreen)
      fillGradient(0, 0, width, height, 0xc0101010, 0xd0101010);
    else fillGradient(0, 0, width, height, 0x80000000, 0xa0000000);
    ci.cancel();
  }
}
