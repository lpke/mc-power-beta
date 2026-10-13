package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.InventoryLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class InventoryLookScreenMixin {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width;
  @Shadow public int height;

  @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true)
  private void power$lookClick(CallbackInfo ci) {
    if (Mouse.getEventButton() != 1 || !Mouse.getEventButtonState()) return;
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if (InventoryLook.outsideGui(minecraft, (Screen)(Object)this, x, y)) ci.cancel();
  }
}
