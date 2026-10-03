package local.luke.creative.mixin;

import local.luke.creative.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.InGame;
import net.minecraft.client.util.ScreenScaler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(InGame.class)
public abstract class CreativeHudMixin extends DrawableHelper {
  @Shadow private Minecraft minecraft;

  @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
      method = "renderHud",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/InGame;blit(IIIIII)V"))
  private void lpke$hotbar(
      InGame hud,
      int x,
      int y,
      int u,
      int v,
      int width,
      int height,
      com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
    if (Modes.spectator(minecraft.player)
        && (height == 22 && (width == 182 || width == 24) || width == 16 && height == 16)) return;
    original.call(hud, x, y, u, v, width, height);
  }

  @Inject(method = "renderHud", at = @At("TAIL"))
  private void lpke$speed(float delta, boolean screen, int x, int y, CallbackInfo ci) {
    String text = ClientRuntime.speedText();
    if (text != null && minecraft.currentScreen == null) {
      ScreenScaler size =
          new ScreenScaler(minecraft.options, minecraft.actualWidth, minecraft.actualHeight);
      drawTextWithShadowCentred(
          minecraft.textRenderer,
          text,
          size.getScaledWidth() / 2,
          size.getScaledHeight() - 60,
          0xFFFFFF);
    }
  }

  @Inject(method = "renderHotbarSlot", at = @At("HEAD"), cancellable = true)
  private void lpke$inventory(int slot, int x, int y, float delta, CallbackInfo ci) {
    if (Modes.spectator(minecraft.player)) ci.cancel();
  }

  @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
  private void lpke$pumpkin(int w, int h, CallbackInfo ci) {
    if (Modes.spectator(minecraft.player)) ci.cancel();
  }
}
