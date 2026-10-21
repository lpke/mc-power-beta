package local.luke.power.offhand;

import local.luke.power.input.InventorySideSlots;
import local.luke.power.inventory.PlayerSideSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public final class OffhandNativeSlot extends Slot implements PlayerSideSlot {
  private final PlayerInventory owner;
  public OffhandNativeSlot(PlayerInventory owner) {
    super(new OffhandSlotInventory(owner), 0, InventorySideSlots.OFFHAND_X, InventorySideSlots.OFFHAND_Y);
    this.owner = owner;
  }
  @Override public boolean canInsert(ItemStack stack) {
    return Offhand.enabled(owner.player)
        && !(owner.player instanceof local.luke.power.input.PlayerMode mode && !mode.power$inventoryEditable());
  }
}
