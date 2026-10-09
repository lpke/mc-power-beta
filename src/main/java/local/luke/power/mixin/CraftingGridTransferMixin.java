package local.luke.power.mixin;

import local.luke.power.inventory.CraftingGridTransfer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public abstract class CraftingGridTransferMixin {
  @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
  private void power$shiftIntoGrid(int slotId, int button, boolean shift, PlayerEntity player,
      CallbackInfoReturnable<ItemStack> ci) {
    if (shift && (button == 0 || button == 1)
        && CraftingGridTransfer.transfer((Container) (Object) this, slotId, player)) ci.setReturnValue(null);
  }
}
