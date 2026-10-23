package local.luke.power.mixin;

import local.luke.power.map.Minimap;
import local.luke.power.map.MinimapQuickMenu;
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
  @Unique private boolean power$resetPress;
  @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true)
  private void power$minimapWheel(CallbackInfo ci) {
    if (!Minimap.menuHost((Screen)(Object)this) || minecraft.currentScreen != (Object)this) return;
    int wheel = Mouse.getEventDWheel();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if(MinimapQuickMenu.mouse(minecraft,x,y,Mouse.getEventButton(),Mouse.getEventButtonState())) {
      InventoryWheel.event(this,false,wheel,true);ci.cancel();return;
    }
    if (!org.lwjgl.opengl.Display.isActive()) power$resetPress = false;
    boolean claimed = Mouse.getEventButton() == 1 && !Mouse.getEventButtonState() && power$resetPress;
    if (Mouse.getEventButton() == 1 && !Mouse.getEventButtonState()) power$resetPress = false;
    if (Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && Minimap.inventoryReset(minecraft, x, y)) {
      power$resetPress = claimed = true;
    }
    if (!claimed) claimed = wheel != 0 && Minimap.inventoryWheel(minecraft, x, y, wheel);
    InventoryWheel.event(this, org.lwjgl.opengl.Display.isActive() && !Mouse.isGrabbed(), wheel, claimed);
    if (claimed) ci.cancel();
  }
  @Inject(method="onKeyboardEvent",at=@At("HEAD"),cancellable=true)
  private void power$minimapKey(CallbackInfo ci) {
    if(MinimapQuickMenu.key(minecraft,org.lwjgl.input.Keyboard.getEventKey(),org.lwjgl.input.Keyboard.getEventKeyState()))ci.cancel();
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
