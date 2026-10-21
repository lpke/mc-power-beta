package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import local.luke.power.input.InventoryGui;
import local.luke.power.input.InventorySideSlots;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_585;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Container;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_585.class)
public abstract class OffhandScreenMixin extends ContainerScreen {
  protected OffhandScreenMixin(Container container) { super(container); }
  @WrapMethod(method="render")
  private void power$slotVisibility(int x,int y,float delta,Operation<Void> original) {
    power$positionSlot();
    original.call(x,y,delta);
  }
  private void power$positionSlot() {
    if(container.slots.size()<=45)return;
    Slot slot=container.method_2084(45);
    if(!(((local.luke.power.mixin.DeathSlotAccessor)slot).power$deathInventory() instanceof OffhandSlotInventory))return;
    boolean visible=Offhand.visible(minecraft.player);
    slot.x=visible?InventorySideSlots.OFFHAND_X:-10000;slot.y=visible?InventorySideSlots.OFFHAND_Y:-10000;

  }
  @Inject(method="drawBackground",at=@At("TAIL"))
  private void power$frame(float delta,CallbackInfo ci) {
    if(!Offhand.visible(minecraft.player)||minecraft.player instanceof local.luke.power.input.PlayerMode mode&&mode.power$creativeInventory())return;
    GL11.glColor4f(1,1,1,1);minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId("/gui/inventory.png"));
    int x=(width-backgroundWidth)/2+local.luke.power.input.InventoryPanels.offset(this), y=(height-backgroundHeight)/2;
    InventorySideSlots.frame(this::fill,x,y,backgroundHeight,false);
    GL11.glColor4f(1,1,1,1);
    drawTexture(x+InventorySideSlots.OFFHAND_X-1,y+InventorySideSlots.OFFHAND_Y-1,7,141,18,18);
  }
}
