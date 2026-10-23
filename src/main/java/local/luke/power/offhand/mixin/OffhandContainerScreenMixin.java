package local.luke.power.offhand.mixin;

import local.luke.power.input.*;
import local.luke.power.offhand.*;
import local.luke.power.inventory.CreativeDeletion;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Container;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public abstract class OffhandContainerScreenMixin extends Screen implements InventoryGui, CreativeDeletion.Host {
  @Shadow public Container container;
  @Shadow protected int backgroundWidth, backgroundHeight;
  @Override public boolean power$deleteSlotAt(int x, int y) {
    return CreativeDeletion.contains(minecraft, (ContainerScreen)(Object)this, backgroundWidth, backgroundHeight, x, y);
  }

  @Unique private boolean power$sideSlotVisible() {
    return minecraft != null && minecraft.player != null
        && OffhandContainers.supported((ContainerScreen)(Object)this)
        && OffhandContainers.slot(container, minecraft.player.inventory) != null
        && Offhand.visible(minecraft.player);
  }

  @Override public boolean power$sideSlotGuiSpace(int px, int py) {
    boolean destroy = CreativeDeletion.enabled(minecraft) && CreativeDeletion.destroySetting();
    if (!power$sideSlotVisible() && !destroy) return false;
    int x = (width - backgroundWidth) / 2 + InventoryPanels.offset(this), y = (height - backgroundHeight) / 2;
    return InventorySideSlots.contains(px, py, x, y, OffhandContainers.frameHeight((ContainerScreen)(Object)this, backgroundHeight),
        OffhandContainers.hotbarY(container, minecraft.player.inventory), power$sideSlotVisible() && destroy);
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
    boolean offhand = power$sideSlotVisible(), destroy = CreativeDeletion.enabled(minecraft) && CreativeDeletion.destroySetting();
    if ((!offhand && !destroy)
        || ((ContainerScreen)(Object)this).getClass() == net.minecraft.class_585.class
            && minecraft.player instanceof PlayerMode mode && mode.power$creativeInventory()) return;
    GL11.glColor4f(1, 1, 1, 1);
    minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(InventorySideSlots.FRAME_TEXTURE));
    int x = (width - backgroundWidth) / 2 + InventoryPanels.offset(this), y = (height - backgroundHeight) / 2;
    int hotbarY = OffhandContainers.hotbarY(container, minecraft.player.inventory);
    InventorySideSlots.frame((px, py, w, h, u, v, sw, sh) ->
        GuiSprite.draw(px, py, w, h, u, v, sw, sh, InventorySideSlots.FRAME_TEXTURE_WIDTH, InventorySideSlots.FRAME_TEXTURE_HEIGHT), x, y, OffhandContainers.frameHeight((ContainerScreen)(Object)this, backgroundHeight), hotbarY, offhand && destroy);
    GL11.glColor4f(1, 1, 1, 1);
    if (offhand) {
      minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(InventorySideSlots.SLOT_TEXTURE));
      GuiSprite.draw(x + InventorySideSlots.SLOT_X, y + hotbarY - 1, 18, 18, 0, 0, 18, 18, 18, 18);
    }
    if (destroy) {
      minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId("/assets/power_creative/textures/inventory.png"));
      GuiSprite.draw(x + InventorySideSlots.SLOT_X, y + hotbarY - (offhand ? 22 : 0) - 1,
          18, 18, 172, 111, 18, 18, 256, 256);
    }
  }
  @Inject(method = "render", at = @At("TAIL"))
  private void power$deleteHint(int x, int y, float delta, CallbackInfo ci) {
    // The native player inventory owns the same hint in its Creative renderer.
    if (((ContainerScreen)(Object)this).getClass() == net.minecraft.class_585.class
        || !CreativeDeletion.clearSetting()
        || !(org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
            || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT))
        || !power$deleteSlotAt(x, y)) return;
    String message = "Clear inventory";
    int textWidth = textRenderer.getWidth(message);
    int left = Math.max(4, Math.min(x + 12, width - textWidth - 4));
    int top = Math.max(4, Math.min(y - 12, height - 12));
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glColor4f(1, 1, 1, 1);
      fillGradient(left - 3, top - 3, left + textWidth + 3, top + 11, 0xc0000000, 0xc0000000);
      textRenderer.drawWithShadow(message, left, top, 0xffffff);
    } finally { GL11.glPopAttrib(); }
  }

}
