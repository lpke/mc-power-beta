package local.luke.power.inventory;

import local.luke.power.input.PlayerMode;
import local.luke.power.inventory.mixin.SlotAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.DispenserScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Repeats normal Shift-clicks. The cursor is never used as temporary storage. */
public final class QuickTransfers {
    private QuickTransfers() {}

    public static boolean eligible(Minecraft mc, ScreenHandler handler) {
        if (mc.player == null || mc.world == null || mc.player.health <= 0 || mc.player.dead
                || mc.player.currentScreenHandler != handler || !handler.canUse(mc.player)
                || mc.player instanceof PlayerMode mode && mode.power$spectator()) return false;
        if (mc.world.isRemote && handler.getClass() == FurnaceScreenHandler.class) return false;
        return handler.getClass() == GenericContainerScreenHandler.class
                || handler.getClass() == FurnaceScreenHandler.class
                || handler.getClass() == DispenserScreenHandler.class;
    }

    public static boolean writable(Slot slot, ItemStack stack) {
        return slot != null && (slot.getClass() == Slot.class || slot instanceof PlayerSideSlot) && stack != null
                && slot.canInsert(stack) && slot.getMaxItemCount() > 0;
    }

    public static Inventory inventory(Slot slot) {
        return ((SlotAccessor) slot).powerInventory$inventory();
    }

    public static void matching(Minecraft mc, ScreenHandler handler, Slot origin, ItemStack cursor) {
        Inventory source = inventory(origin);
        var contents = InventoryStacks.data(cursor);
        // Snapshot slot references before native callbacks; never scan newly created slots.
        Object[] slots = handler.slots.toArray();
        for (Object candidate : slots) {
            if (!eligible(mc, handler) || mc.player.inventory.getCursorStack() != cursor
                    || !contents.equals(InventoryStacks.data(cursor))) break;
            Slot slot = (Slot) candidate;
            if (inventory(slot) != source || !writable(slot, cursor)
                    || !InventoryStacks.same(cursor, slot.getStack())) continue;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, true, mc.player);
        }
    }
}
