package local.luke.power.mixin;

import local.luke.power.ui.TextDrag;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class TextSelectionMixin {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width;
  @Inject(method = "onMouseEvent", at = @At("HEAD"))
  private void power$context(CallbackInfo ci) {
    TextDrag.context(minecraft.currentScreen, Display.isActive());
  }
  @Inject(method = "onMouseEvent", at = @At("TAIL"))
  private void power$drag(CallbackInfo ci) {
    TextDrag.event(minecraft.currentScreen, Mouse.getEventX() * width / minecraft.displayWidth,
        Mouse.getEventButton(), Mouse.getEventButtonState(), Display.isActive());
  }
}
