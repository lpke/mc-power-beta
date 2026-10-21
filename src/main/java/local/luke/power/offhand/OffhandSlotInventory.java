package local.luke.power.offhand;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** Dedicated native inventory, index zero. PlayerInventory.size stays forty. */
public final class OffhandSlotInventory implements Inventory {
  private final PlayerInventory owner;
  public OffhandSlotInventory(PlayerInventory owner) { this.owner = owner; }
  public PlayerInventory owner() { return owner; }
  private static void index(int index) { if (index != 0) throw new IndexOutOfBoundsException(index); }
  public int size() { return 1; }
  public ItemStack getStack(int index) { index(index); return Offhand.get(owner); }
  public ItemStack removeStack(int index, int amount) {
    index(index); ItemStack stack = Offhand.get(owner);
    if (stack == null || amount <= 0) return null;
    if (amount >= stack.count) { Offhand.set(owner, null); return stack; }
    ItemStack result = stack.clone(); result.count = amount; stack.count -= amount; markDirty(); return result;
  }
  public void setStack(int index, ItemStack stack) { index(index); Offhand.set(owner, stack); }
  public String getName() { return "Offhand"; }
  public int getMaxCountPerStack() { return owner.getMaxCountPerStack(); }
  public void markDirty() { owner.markDirty(); }
  public boolean canPlayerUse(PlayerEntity player) { return player == owner.player && !player.world.isRemote; }
}
