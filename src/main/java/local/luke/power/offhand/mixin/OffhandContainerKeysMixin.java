package local.luke.power.offhand.mixin;

import local.luke.power.input.*;
import local.luke.power.offhand.*;
import local.luke.power.mixin.ButtonBoundsAccessor;
import net.minecraft.*;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native inventory key ownership, including hovered-slot swaps in player/container screens. */
@Mixin(ContainerScreen.class)
public abstract class OffhandContainerKeysMixin extends net.minecraft.client.gui.screen.Screen implements OffhandSlotPicker {
  @Unique private int power$pointerX=-10000,power$pointerY=-10000;
  @Invoker("getSlotAt") public abstract Slot power$offhandSlotAt(int x,int y);
  public int power$offhandPointerX(){return power$pointerX;}
  public int power$offhandPointerY(){return power$pointerY;}
  public void power$offhandPointer(int x,int y){power$pointerX=x;power$pointerY=y;}
  public boolean power$offhandWidgetAt(int x,int y) {
    for(Object entry:buttons) if(entry instanceof net.minecraft.client.gui.widget.ButtonWidget button && button.visible) {
      var bounds=(ButtonBoundsAccessor)(Object)button;
      if(x>=button.x&&x<button.x+bounds.power$width()&&y>=button.y&&y<button.y+bounds.power$height())return true;
    }
    return false;
  }
  @Inject(method="render",at=@At("HEAD"))
  private void power$pointer(int x,int y,float delta,CallbackInfo ci){power$pointerX=x;power$pointerY=y;}
  @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
  private void power$swap(char character,int key,CallbackInfo ci) {
    if(Keyboard.isRepeatEvent()||!Bindings.matches("powerbeta.swapHands",key))return;
    Slot slot=OffhandInput.hovered(minecraft);if(slot==null)return;
    Offhand.swapSlot(minecraft,slot);ci.cancel();
  }
}
