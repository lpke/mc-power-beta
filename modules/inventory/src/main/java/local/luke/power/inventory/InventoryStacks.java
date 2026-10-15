package local.luke.power.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native serialization includes extension data, such as item names and StationAPI NBT. */
public final class InventoryStacks {
    private InventoryStacks() {}

    public static NbtCompound data(ItemStack stack) {
        if (stack == null) return null;
        NbtCompound tag = stack.writeNbt(new NbtCompound());
        tag.putInt("Count", stack.count);
        return (NbtCompound) tag.copy();
    }

    public static boolean same(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == b;
        if (a.itemId != b.itemId || a.getDamage() != b.getDamage()) return false;
        NbtCompound left = a.writeNbt(new NbtCompound());
        NbtCompound right = b.writeNbt(new NbtCompound());
        left.putByte("Count", (byte) 1);
        right.putByte("Count", (byte) 1);
        return left.equals(right);
    }

    public static ItemStack copy(ItemStack stack, int count) {
        if (stack == null || count <= 0) return null;
        ItemStack result = new ItemStack((NbtCompound) stack.writeNbt(new NbtCompound()).copy());
        result.count = count;
        return result;
    }
}
