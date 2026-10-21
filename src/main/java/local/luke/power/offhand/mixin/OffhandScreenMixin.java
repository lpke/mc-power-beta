package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import local.luke.power.input.InventoryGui;
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
  @Inject(method="render",at=@At("HEAD"))
  private void power$slotVisibility(int x,int y,float delta,CallbackInfo ci) {
    if(container.slots.size()<=45)return;
    Slot slot=container.method_2084(45);
    if(!(((local.luke.power.mixin.DeathSlotAccessor)slot).power$deathInventory() instanceof OffhandSlotInventory))return;
    boolean visible=Offhand.visible(minecraft.player)&&!((Object)this instanceof InventoryGui gui&&gui.power$usesCatalogWheel());
    slot.x=visible?77:-10000;slot.y=visible?62:-10000;
  }
  @Inject(method="drawBackground",at=@At("TAIL"))
  private void power$frame(float delta,CallbackInfo ci) {
    if(!Offhand.visible(minecraft.player)||(Object)this instanceof InventoryGui gui&&gui.power$usesCatalogWheel())return;
    GL11.glColor4f(1,1,1,1);minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId("/gui/inventory.png"));
    drawTexture((width-backgroundWidth)/2+local.luke.power.input.InventoryPanels.offset(this)+76,(height-backgroundHeight)/2+61,7,83,18,18);
  }
}
