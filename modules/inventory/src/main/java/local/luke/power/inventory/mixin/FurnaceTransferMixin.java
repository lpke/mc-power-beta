package local.luke.power.inventory.mixin;

import local.luke.power.inventory.Config;
import local.luke.power.inventory.ModHelperStationAPI;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceScreenHandler.class)
public abstract class FurnaceTransferMixin extends ScreenHandler {
    @Shadow private FurnaceBlockEntity furnaceBlockEntity;

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void powerInventory$furnace(int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableShiftClickingItemsIntoFurnaces
                || furnaceBlockEntity.world == null || furnaceBlockEntity.world.isRemote
                || index < 3 || index >= slots.size()) return;
        Slot source = (Slot) slots.get(index);
        ItemStack stack = source.getStack();
        if (stack == null) return;
        int target = ModHelperStationAPI.getResultFor(stack) != null ? 0
                : ModHelperStationAPI.getFuelTime(stack) > 0 ? 1 : -1;
        if (target < 0) return;
        ItemStack before = stack.copy();
        insertItem(stack, target, target + 1, false);
        if (stack.count == before.count) { cir.setReturnValue(null); return; }
        if (stack.count == 0) source.setStack(null); else source.markDirty();
        source.onTakeItem(stack);
        cir.setReturnValue(before);
    }
}
