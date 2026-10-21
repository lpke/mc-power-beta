package local.luke.power.offhand.mixin;

import local.luke.power.input.*;
import local.luke.power.offhand.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Container;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public abstract class OffhandContainerScreenMixin extends Screen implements InventoryGui {
  @Shadow public Container container;
  @Shadow protected int backgroundWidth, backgroundHeight;

  @Unique private boolean power$sideSlotVisible() {
    return minecraft != null && minecraft.player != null
        && OffhandContainers.supported((ContainerScreen)(Object)this)
        && OffhandContainers.slot(container, minecraft.player.inventory) != null
        && Offhand.visible(minecraft.player);
  }

  @Override public boolean power$sideSlotGuiSpace(int px, int py) {
    if (!power$sideSlotVisible()) return false;
    int x = (width - backgroundWidth) / 2 + InventoryPanels.offset(this), y = (height - backgroundHeight) / 2;
    return InventorySideSlots.contains(px, py, x, y, OffhandContainers.frameHeight((ContainerScreen)(Object)this, backgroundHeight),
        OffhandContainers.hotbarY(container, minecraft.player.inventory), false);
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void power$positionSlot(int x, int y, float delta, CallbackInfo ci) {
    if (minecraft.player == null) return;
    var slot = OffhandContainers.slot(container, minecraft.player.inventory);
    if (slot == null) return;
    boolean visible = power$sideSlotVisible();
    slot.x = visible ? InventorySideSlots.OFFHAND_X : -10000;
    slot.y = visible ? OffhandContainers.hotbarY(container, minecraft.player.inventory) : -10000;
  }

  @Inject(method = "render", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/screen/container/ContainerScreen;drawBackground(F)V", shift = At.Shift.AFTER))
  private void power$frame(int pointerX, int pointerY, float delta, CallbackInfo ci) {
    if (!power$sideSlotVisible()
        || ((ContainerScreen)(Object)this).getClass() == net.minecraft.class_585.class
            && minecraft.player instanceof PlayerMode mode && mode.power$creativeInventory()) return;
    GL11.glColor4f(1, 1, 1, 1);
    minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId("/gui/inventory.png"));
    int x = (width - backgroundWidth) / 2 + InventoryPanels.offset(this), y = (height - backgroundHeight) / 2;
    int hotbarY = OffhandContainers.hotbarY(container, minecraft.player.inventory);
    InventorySideSlots.frame(this::fill, x, y, OffhandContainers.frameHeight((ContainerScreen)(Object)this, backgroundHeight), hotbarY, false);
    GL11.glColor4f(1, 1, 1, 1);
    drawTexture(x + InventorySideSlots.OFFHAND_X - 1, y + hotbarY - 1, 7, 141, 18, 18);
  }
}
