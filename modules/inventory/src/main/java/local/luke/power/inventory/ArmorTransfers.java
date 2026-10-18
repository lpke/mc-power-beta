package local.luke.power.inventory;

import java.util.Objects;
import local.luke.power.controls.mixininterface.ControlFeaturesArmorSlot;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Equip through ordinary clicks, including the native one-item armour-slot limit. */
public final class ArmorTransfers {
    private ArmorTransfers() {}

    public static boolean armorSlot(ScreenHandler handler, Slot slot) {
        return handler.getClass() == PlayerScreenHandler.class && slot != null
                && slot.id >= 5 && slot.id <= 8 && handler.slots.get(slot.id) == slot
                && slot.getClass().getSuperclass() == Slot.class
                && slot instanceof ControlFeaturesArmorSlot armor && armor.powerControls$isArmorSlot();
    }

    public static boolean equip(Minecraft mc, ScreenHandler handler, Slot source, int button) {
        if (!owns(mc, handler)
                || source == null || source.getClass() != Slot.class || !source.hasStack()
                || source.id < 9 || source.id >= handler.slots.size() || handler.slots.get(source.id) != source
                || QuickTransfers.inventory(source) != mc.player.inventory
                || button < 0 || button > 1) return false;
        var screen = mc.currentScreen;
        ItemStack stack = source.getStack();
        int equipment = stack.getItem() instanceof ArmorItem armor ? armor.equipmentSlot
                : stack.itemId == Block.PUMPKIN.id ? 0 : -1;
        if (equipment < 0 || equipment > 3) return false;
        Slot target = (Slot) handler.slots.get(5 + equipment);
        if (!armorSlot(handler, target) || target.hasStack() || !target.canInsert(stack) || target.getMaxItemCount() < 1) return false;
        var sourceData = InventoryStacks.data(stack);
        var equippedData = InventoryStacks.data(InventoryStacks.copy(stack, 1));
        var cursorData = InventoryStacks.data(mc.player.inventory.getCursorStack());
        ItemStack rest = stack.count > 1 ? InventoryStacks.copy(stack, stack.count - 1) : null;
        mc.interactionManager.clickSlot(handler.syncId, source.id, 0, false, mc.player);
        // Native callbacks may change these slots. Never overwrite their result blindly.
        if (!owns(mc, handler) || mc.currentScreen != screen
                || !Objects.equals(InventoryStacks.data(mc.player.inventory.getCursorStack()), sourceData)
                || !Objects.equals(InventoryStacks.data(source.getStack()), cursorData) || target.hasStack()) return true;
        mc.interactionManager.clickSlot(handler.syncId, target.id, 0, false, mc.player);
        if (!owns(mc, handler) || mc.currentScreen != screen
                || !Objects.equals(InventoryStacks.data(target.getStack()), equippedData)
                || !Objects.equals(InventoryStacks.data(mc.player.inventory.getCursorStack()), InventoryStacks.data(rest))
                || !Objects.equals(InventoryStacks.data(source.getStack()), cursorData)) return true;
        mc.interactionManager.clickSlot(handler.syncId, source.id, 0, false, mc.player);
        return true;
    }

    private static boolean owns(Minecraft mc, ScreenHandler handler) {
        return mc.currentScreen instanceof InventoryScreen && handler.getClass() == PlayerScreenHandler.class
                && mc.world != null && mc.player != null && mc.player.world == mc.world
                && !mc.player.dead && mc.player.health > 0 && mc.player.currentScreenHandler == handler
                && !(mc.player instanceof PlayerMode mode && mode.power$spectator()) && handler.canUse(mc.player);
    }
}
