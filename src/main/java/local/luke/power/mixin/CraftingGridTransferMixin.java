package local.luke.power.mixin;

import local.luke.power.inventory.CraftingGridTransfer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Container.class)
public abstract class CraftingGridTransferMixin {
  @Unique private int power$slotClickDepth;

  @WrapMethod(method = "onSlotClick")
  private ItemStack power$shiftIntoGrid(int slotId, int button, boolean shift, PlayerEntity player,
      Operation<ItemStack> original) {
    boolean outermost = power$slotClickDepth++ == 0;
    try {
      // Native Shift-click retries its own remainder after a partial storage merge.
      // That retry belongs to the same click and must not start a new grid transfer.
      if (outermost && shift && (button == 0 || button == 1)
          && CraftingGridTransfer.transfer((Container) (Object) this, slotId, player)) return null;
      return original.call(slotId, button, shift, player);
    } finally {
      power$slotClickDepth--;
    }
  }
}
