package local.luke.power.creative.inventory.mixin.client;

import local.luke.power.creative.inventory.interfaces.CreativeDragScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
  @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
  private void power$cancelPaintOnResize(Minecraft mc, int width, int height, CallbackInfo ci) {
    if ((Object)this instanceof CreativeDragScreen screen) screen.power$cancelCreativePaint();
  }
}
