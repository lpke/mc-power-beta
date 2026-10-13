package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.InventoryLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class InventoryLookScreenMixin {
  @Shadow protected Minecraft minecraft;

  @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true)
  private void power$lookClick(CallbackInfo ci) {
    if (InventoryLook.mouseEvent(minecraft, (Screen)(Object)this)) ci.cancel();
  }

  @Inject(method = "tickInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", ordinal = 0))
  private void power$mouseBatch(CallbackInfo ci) { InventoryLook.finishEvents(); }

  @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "onMouseEvent",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventX()I"))
  private int power$cursorX(int x) { return InventoryLook.eventX(x); }

  @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "onMouseEvent",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventY()I"))
  private int power$cursorY(int y) { return InventoryLook.eventY(y); }
}
