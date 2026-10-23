package local.luke.power.creative.inventory.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.creative.inventory.CreativeStackDrag;
import local.luke.power.creative.inventory.interfaces.CreativeDragScreen;
import local.luke.power.input.Bindings;
import local.luke.power.input.InventoryDrag;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.container.slot.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContainerScreen.class, priority = 1100)
public abstract class ContainerScreenMixin extends Screen implements CreativeDragScreen {
  @Shadow public Container container;
  @Shadow protected abstract Slot getSlot(int x, int y);
  @Unique private final CreativeStackDrag power$paint = new CreativeStackDrag();

  @Inject(method = "getSlot", at = @At("RETURN"), cancellable = true)
  private void power$hideCatalogBackgroundSlots(int x, int y, CallbackInfoReturnable<Slot> ci) {
    if ((Object)this instanceof local.luke.power.input.InventoryGui gui && gui.power$usesCatalogWheel()
        && ci.getReturnValue() != null && ci.getReturnValue().id < 36) ci.setReturnValue(null);
  }

  @Override public Slot power$defaultCreativePaintSlot(int x, int y) { return getSlot(x, y); }
  @Override public Slot power$creativePaintSlot(int x, int y) { return getSlot(x, y); }

  @Override public boolean power$creativePaintClick(int x, int y, int button) {
    if (Bindings.matches(minecraft.options.inventoryKey, button - 100)) {
      power$paint.cancel();
      return false;
    }
    if (power$paint.active()) { power$paint.cancel(); return true; }
    if (button != 2 || minecraft.player == null || minecraft.player.inventory.getCursorItem() == null
        || !power$paint.allowed(minecraft, (ContainerScreen)(Object)this, container)) return false;
    if ((Object)this instanceof InventoryDrag drag) drag.power$cancelItemDrag();
    return power$paint.begin(minecraft, (ContainerScreen)(Object)this, container);
  }

  @Override public void power$cancelCreativePaint() { power$paint.cancel(); }

  @Override public ItemStack power$creativePaintPreview(Slot slot, ItemStack original) {
    return power$paint.preview(minecraft, (ContainerScreen)(Object)this, container, slot, original);
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void power$middleClick(int x, int y, int button, CallbackInfo ci) {
    if (power$creativePaintClick(x, y, button)) ci.cancel();
  }

  @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
  private void power$middleDrag(int x, int y, int button, CallbackInfo ci) {
    if (!power$paint.active()) return;
    if (button == -1) {
      if (Mouse.isButtonDown(2)) power$paint.hover(minecraft, (ContainerScreen)(Object)this,
          container, power$creativePaintSlot(x, y));
    } else if (button == 2) power$paint.finish(minecraft, (ContainerScreen)(Object)this, container);
    else power$paint.cancel();
    ci.cancel();
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$checkPaint(CallbackInfo ci) {
    if (power$paint.active() && !power$paint.valid(minecraft, (ContainerScreen)(Object)this, container)) power$paint.cancel();
  }

  @Inject(method = "onClose()V", at = @At("HEAD"))
  private void power$cancelPaint(CallbackInfo ci) { power$paint.cancel(); }

  @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/container/slot/Slot;getItem()Lnet/minecraft/item/ItemStack;"))
  private ItemStack power$previewPaint(Slot slot, Operation<ItemStack> original) {
    return power$creativePaintPreview(slot, original.call(slot));
  }
}
