package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
  @WrapMethod(method="method_671")
  private boolean power$includeOffhandPickup(ItemStack incoming, Operation<Boolean> original) {
    int before = incoming == null ? 0 : incoming.count;
    boolean inserted = original.call(incoming);
    return inserted || incoming != null && incoming.count < before;
  }
  @Inject(method="method_671", at=@At("HEAD"), cancellable=true)
  private void power$refillOccupiedOffhand(ItemStack incoming, CallbackInfoReturnable<Boolean> cir) {
    if (!Offhand.enabled(player) || incoming == null || incoming == power$offhand || power$offhand == null
        || incoming.count <= 0 || !incoming.isStackable() || !incoming.isItemEqual(power$offhand)) return;
    var incomingData = incoming.writeNbt(new net.minecraft.nbt.NbtCompound());
    var offhandData = power$offhand.writeNbt(new net.minecraft.nbt.NbtCompound());
    incomingData.putByte("Count", (byte)1); offhandData.putByte("Count", (byte)1);
    if (!incomingData.equals(offhandData)) return;
    int amount = Math.min(incoming.count, Math.max(0,
        Math.min(incoming.getMaxCount(), ((PlayerInventory)(Object)this).getMaxCountPerStack()) - power$offhand.count));
    if (amount <= 0) return;
    power$offhand.count += amount; power$offhand.field_752 = 5; incoming.count -= amount;
    ((PlayerInventory)(Object)this).markDirty();
    if (incoming.count == 0) cir.setReturnValue(true);
  }
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
