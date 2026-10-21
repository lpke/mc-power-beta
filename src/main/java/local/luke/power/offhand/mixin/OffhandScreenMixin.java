package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import local.luke.power.input.InventorySideSlots;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_585;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Container;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(class_585.class)
public abstract class OffhandScreenMixin extends ContainerScreen {
  protected OffhandScreenMixin(Container container) { super(container); }
  @WrapMethod(method="render")
  private void power$slotVisibility(int x,int y,float delta,Operation<Void> original) {
    power$positionSlot();
    original.call(x,y,delta);
  }
  private void power$positionSlot() {
    Slot slot=OffhandContainers.slot(container,minecraft.player.inventory);
    if(slot==null)return;
    boolean visible=Offhand.visible(minecraft.player);
    slot.x=visible?InventorySideSlots.OFFHAND_X:-10000;
    slot.y=visible?OffhandContainers.hotbarY(container,minecraft.player.inventory):-10000;
  }
}
