package local.luke.power.inventory.tools.mixin;

import local.luke.power.input.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Screen.class, priority = 1300)
public abstract class InventoryToolsMouseMixin {
  @Shadow protected Minecraft minecraft;
  @Shadow public int width, height;
  @Unique private final java.util.BitSet power$ownedMouseButtons = new java.util.BitSet(100);
  @Inject(method = "onMouseEvent", at = @At("HEAD"), cancellable = true)
  private void power$inventoryMouse(CallbackInfo ci) {
    int button = Mouse.getEventButton();
    if (button >= 0 && button < 100 && !Mouse.getEventButtonState() && power$ownedMouseButtons.get(button)) {
      power$ownedMouseButtons.clear(button); ci.cancel(); return;
    }
    if ((Object)this instanceof InventoryToolsHost host && host.power$inventoryToolsModal() && Mouse.getEventDWheel() != 0) {
      InventoryWheel.event(this, Display.isActive() && !Mouse.isGrabbed(), Mouse.getEventDWheel(), true); ci.cancel(); return;
    }
    if (button < 0 || button >= 100 || !Mouse.getEventButtonState() || minecraft == null || minecraft.player == null
        || minecraft.currentScreen != (Object)this || !(this instanceof OffhandSlotPicker)
        || minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return;
    var settings = InventoryToolsConfig.current();
    if (!Offhand.enabled(minecraft.player) && !settings.sorting && !settings.depositMatching || !power$hasMouseBinding(button - 100)
        || !OffhandInput.container(minecraft)) return;
    int x = Mouse.getEventX() * width / minecraft.displayWidth, y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    var picker = (OffhandSlotPicker)(Object)this; picker.power$offhandPointer(x, y);
    var slot = OffhandInput.pick(minecraft, x, y); if (slot == null) return;
    int code = button - 100;
    if (Bindings.matches("powerbeta.swapHands", code) && OffhandInput.swapEligible(minecraft, slot)) Offhand.swapSlot(minecraft, slot);
    else if (Bindings.matches("powerbeta.sortInventory", code) && InventoryToolsConfig.current().sorting && SortingSlots.eligible(minecraft))
      SortingActions.sort(minecraft, local.luke.power.ui.TextEditing.shift());
    else if (Bindings.matches("powerbeta.depositMatching", code) && InventoryToolsConfig.current().depositMatching && SortingSlots.eligible(minecraft))
      SortingActions.depositMatching(minecraft);
    else return;
    power$ownedMouseButtons.set(button); ci.cancel();
  }
  @Unique private boolean power$hasMouseBinding(int code) {
    if (minecraft.options == null) return false;
    for (Object entry : minecraft.options.allKeys) if (entry instanceof Binding binding && binding.power$code() == code
        && (binding.power$id().equals("powerbeta.swapHands") || binding.power$id().equals("powerbeta.sortInventory")
            || binding.power$id().equals("powerbeta.depositMatching"))) return true;
    return false;
  }
}
