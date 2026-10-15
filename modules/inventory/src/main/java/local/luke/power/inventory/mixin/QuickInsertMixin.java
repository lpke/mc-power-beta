package local.luke.power.inventory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.InventoryStacks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.DispenserScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ScreenHandler.class)
public abstract class QuickInsertMixin {
    @Shadow public List slots;
    @Unique private boolean powerInventory$localClick;

    @WrapMethod(method = "onSlotClick")
    private ItemStack powerInventory$local(int slot, int button, boolean shift, PlayerEntity player,
                                         Operation<ItemStack> original) {
        boolean previous = powerInventory$localClick;
        powerInventory$localClick = player.world != null && !player.world.isRemote;
        try { return original.call(slot, button, shift, player); }
        finally { powerInventory$localClick = previous; }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void powerInventory$insert(ItemStack stack, int start, int end, boolean reverse, CallbackInfo ci) {
        Class<?> type = getClass();
        if (type != GenericContainerScreenHandler.class && type != FurnaceScreenHandler.class
                && type != DispenserScreenHandler.class) return;
        if (!powerInventory$localClick || stack == null || start < 0 || end > slots.size() || start >= end) return;
        // Keep native ordering, but respect slot limits and every piece of saved item data.
        for (boolean empty : new boolean[]{false, true}) {
            for (int i = reverse ? end - 1 : start; i >= start && i < end && stack.count > 0; i += reverse ? -1 : 1) {
                Slot slot = (Slot) slots.get(i);
                ItemStack existing = slot.getStack();
                if ((existing == null) != empty || !slot.canInsert(stack)) continue;
                if (!empty && (!stack.isStackable() || !InventoryStacks.same(stack, existing))) continue;
                int capacity = Math.min(stack.getMaxCount(), slot.getMaxItemCount()) - (empty ? 0 : existing.count);
                int amount = Math.min(stack.count, Math.max(0, capacity));
                if (amount == 0) continue;
                if (empty) slot.setStack(stack.split(amount));
                else { existing.count += amount; stack.count -= amount; slot.markDirty(); }
            }
        }
        ci.cancel();
    }
}
