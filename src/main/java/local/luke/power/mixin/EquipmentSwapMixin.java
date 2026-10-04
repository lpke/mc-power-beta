package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Container.class)
public abstract class EquipmentSwapMixin {
  /** Make vanilla compare durability in its accepted-slot swap branch. No custom
   * item transfers, cloning, count changes or replacement NBT are involved. */
  @WrapOperation(method = "onSlotClick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/item/ItemStack;method_719()Z", ordinal = 0))
  private boolean power$compareDurability(ItemStack slot, Operation<Boolean> original,
      int slotId, int button, boolean shift, PlayerEntity player) {
    if (original.call(slot)) return true;
    if (!VisualConfig.current().swapEquipment || player.world == null || player.world.isRemote)
      return false;
    ItemStack cursor = player.inventory.getCursorStack();
    return cursor != null && cursor != slot && slot.count == 1 && cursor.count == 1
        && slot.getMaxCount() == 1 && cursor.getMaxCount() == 1 && slot.isDamageable();
  }
}
