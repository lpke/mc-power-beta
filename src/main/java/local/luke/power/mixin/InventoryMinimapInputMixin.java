package local.luke.power.mixin;

import local.luke.power.map.Minimap;
import local.luke.power.input.InventoryGui;
import local.luke.power.input.InventoryWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Container panels retain ownership when they cover the minimap. */
@Mixin(value = Screen.class, priority = 900)
public abstract class InventoryMinimapInputMixin {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width;
  @Shadow public int height;
  @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true)
  private void power$minimapWheel(CallbackInfo ci) {
    if (!((Object)this instanceof ContainerScreen) || minecraft.currentScreen != (Object)this) return;
    int wheel = Mouse.getEventDWheel();
    boolean claimed = wheel != 0 && Minimap.inventoryWheel(minecraft, Mouse.getEventX() * width / minecraft.displayWidth,
        height - Mouse.getEventY() * height / minecraft.displayHeight - 1, wheel);
    InventoryWheel.event(this, org.lwjgl.opengl.Display.isActive() && !Mouse.isGrabbed(), wheel, claimed);
    if (claimed) ci.cancel();
  }
  @Inject(method = "onMouseEvent", at = @At("RETURN"))
  private void power$finishWheel(CallbackInfo ci) {
    if ((Object)this instanceof ContainerScreen && minecraft.currentScreen == (Object)this)
      InventoryWheel.finishEvent(this, (Object)this instanceof InventoryGui gui && gui.power$usesCatalogWheel());
  }
  @Inject(method = "tickInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", ordinal = 0))
  private void power$finishMouseBatch(CallbackInfo ci) {
    // Every queued wheel amount has its own owner now. Clear only after the complete mouse batch,
    // before a keyboard action can open a screen that still uses native aggregate polling.
    if ((Object)this instanceof ContainerScreen && minecraft.currentScreen == (Object)this) Mouse.getDWheel();
  }
}
