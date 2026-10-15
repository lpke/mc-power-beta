package local.luke.power.inventory;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

/** Only a gesture's own unchanged writes may be redistributed or rolled back. */
public final class DragOwnership {
    private record StackState(ItemStack identity, NbtCompound data) {
        StackState(ItemStack stack) { this(stack, InventoryStacks.data(stack)); }
        boolean matches(ItemStack stack) {
            return identity == stack && Objects.equals(data, InventoryStacks.data(stack));
        }
    }

    private Object owner;
    private StackState cursor;
    private final Map<Slot, StackState> slots = new IdentityHashMap<>();

    public void capture(Object owner, ItemStack cursor, Iterable<Slot> visited) {
        this.owner = owner;
        this.cursor = new StackState(cursor);
        slots.clear();
        for (Slot slot : visited) if (slot != null) slots.put(slot, new StackState(slot.getStack()));
    }

    public boolean current(Object owner, ItemStack cursor) {
        if (this.owner == null) return true;
        if (this.owner != owner || !this.cursor.matches(cursor)) return false;
        for (var entry : slots.entrySet()) if (!entry.getValue().matches(entry.getKey().getStack())) return false;
        return true;
    }

    public void clear() { owner = null; cursor = null; slots.clear(); }
}
