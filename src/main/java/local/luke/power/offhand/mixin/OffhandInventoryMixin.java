package local.luke.power.offhand.mixin;

import local.luke.power.inventory.*;
import local.luke.power.offhand.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(PlayerInventory.class)
public class OffhandInventoryMixin implements OffhandInventory<ItemStack> {
  @Shadow public int selectedSlot;
  @Shadow public net.minecraft.entity.player.PlayerEntity player;
  @Unique private ItemStack power$offhand;
  public ItemStack power$offhandStack() { return power$offhand; }
  public void power$offhandStack(ItemStack value) { power$offhand = value; }
  public boolean power$offhandEnabled() { return Offhand.enabled(player); }
  public void power$offhandChanged() { OffhandRenderer.changed(); }
  @Inject(method="getSelectedItem", at=@At("HEAD"), cancellable=true)
  private void power$effectiveHand(CallbackInfoReturnable<ItemStack> cir) {
    if (HandUse.hand(this) == HandUse.Hand.OFF) cir.setReturnValue(power$offhand);
  }
  @Inject(method="setStack", at=@At("HEAD"), cancellable=true)
  private void power$selectedCallback(int slot, ItemStack stack, CallbackInfo ci) {
    if (slot == selectedSlot && HandUse.routesSelectedSetter(this)) {
      power$offhand = stack; ((PlayerInventory)(Object)this).markDirty(); ci.cancel();
    }
  }
  @Inject(method="method_686", at=@At("TAIL"))
  private void power$tickHeld(CallbackInfo ci) {
    if (Offhand.enabled(player) && power$offhand != null) power$offhand.method_699(player.world, player, 40, true);
  }
}
