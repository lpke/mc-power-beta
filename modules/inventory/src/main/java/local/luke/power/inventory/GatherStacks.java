package local.luke.power.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import local.luke.power.input.PlayerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Consolidates through normal clicks, so multiplayer servers retain authority. */
public final class GatherStacks {
    private GatherStacks() {}

    public static void gather(Minecraft mc, ScreenHandler handler, Slot origin) {
        ItemStack held = mc.player.inventory.getCursorStack();
        if (!eligible(mc, handler) || held == null || held.count <= 0 || held.count >= held.getMaxCount()) return;
        Object screen = mc.currentScreen;
        int maximum = held.getMaxCount();
        // The first pickup normally leaves the required temporary slot. A restricted
        // container may need an empty player slot instead. Never borrow occupied slots.
        Slot buffer = emptyBuffer(handler, origin, held, maximum);
        if (buffer == null) return;
        var sources = new ArrayList<Slot>();
        for (Object entry : handler.slots.toArray()) {
            Slot slot = (Slot) entry;
            if (slot != buffer && QuickTransfers.writable(slot, held)
                    && InventoryStacks.same(held, slot.getStack()) && slot.getStack().count > 0
                    && slot.getStack().count <= Math.min(maximum, slot.getMaxItemCount())) sources.add(slot);
        }
        sources.sort(Comparator.comparingInt(slot -> priority(mc, slot)));
        for (Slot source : sources) {
            held = mc.player.inventory.getCursorStack();
            if (mc.currentScreen != screen || !eligible(mc, handler) || held == null || held.count >= maximum
                    || buffer.hasStack() || !QuickTransfers.writable(buffer, held)) break;
            ItemStack stack = source.getStack();
            if (!QuickTransfers.writable(source, held) || !InventoryStacks.same(held, stack)
                    || stack.count <= 0 || stack.count > Math.min(maximum, source.getMaxItemCount())) continue;
            // Immutable native snapshots revalidate every callback, including full NBT.
            ItemStack before = InventoryStacks.copy(held, held.count);
            ItemStack from = InventoryStacks.copy(stack, stack.count);
            int total = before.count + from.count;
            ItemStack combined = InventoryStacks.copy(before, Math.min(maximum, total));
            ItemStack remainder = InventoryStacks.copy(from, total - combined.count);
            if (!click(mc, handler, screen, buffer, null, source, from, buffer, before)) break;
            if (!click(mc, handler, screen, source, from, source, null, buffer, before)) break;
            if (!click(mc, handler, screen, buffer, remainder, source, null, buffer, combined)) break;
            if (remainder != null && !click(mc, handler, screen, source, null, source, remainder, buffer, combined)) break;
            if (!click(mc, handler, screen, buffer, combined, source, remainder, buffer, null)) break;
        }
    }

    private static int priority(Minecraft mc, Slot slot) {
        int owner = QuickTransfers.inventory(slot) == mc.player.inventory ? 0 : 2;
        return owner + (slot.getStack().count < Math.min(slot.getMaxItemCount(), slot.getStack().getMaxCount()) ? 0 : 1);
    }

    private static Slot emptyBuffer(ScreenHandler handler, Slot origin, ItemStack held, int maximum) {
        if (!origin.hasStack() && QuickTransfers.writable(origin, held) && origin.getMaxItemCount() >= maximum) return origin;
        for (Object entry : handler.slots) {
            Slot slot = (Slot) entry;
            if (!slot.hasStack() && QuickTransfers.writable(slot, held) && slot.getMaxItemCount() >= maximum) return slot;
        }
        return null;
    }

    private static boolean click(Minecraft mc, ScreenHandler handler, Object screen, Slot target, ItemStack cursor,
                                 Slot source, ItemStack sourceAfter, Slot buffer, ItemStack bufferAfter) {
        if (mc.currentScreen != screen || !eligible(mc, handler)
                || !registered(handler, source) || !registered(handler, buffer)) return false;
        mc.interactionManager.clickSlot(handler.syncId, target.id, 0, false, mc.player);
        return mc.currentScreen == screen && eligible(mc, handler)
                && registered(handler, source) && registered(handler, buffer)
                && equal(mc.player.inventory.getCursorStack(), cursor)
                && equal(source.getStack(), sourceAfter) && equal(buffer.getStack(), bufferAfter);
    }

    private static boolean registered(ScreenHandler handler, Slot slot) {
        return slot.id >= 0 && slot.id < handler.slots.size() && handler.slots.get(slot.id) == slot;
    }

    private static boolean equal(ItemStack actual, ItemStack expected) {
        return actual == null ? expected == null : expected != null && actual.count == expected.count
                && InventoryStacks.same(actual, expected);
    }

    private static boolean eligible(Minecraft mc, ScreenHandler handler) {
        return mc.world != null && mc.player != null && !mc.player.dead && mc.player.health > 0
                && mc.player.currentScreenHandler == handler && handler.canUse(mc.player)
                && !(mc.player instanceof PlayerMode mode && mode.power$spectator());
    }
}
