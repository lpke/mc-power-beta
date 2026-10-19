package local.luke.power.creative.inventory.interfaces;

import net.minecraft.container.slot.Slot;
import net.minecraft.item.ItemStack;

/** Shared hooks for normal containers and the creative catalog's visible hotbar. */
public interface CreativeDragScreen {
  boolean power$creativePaintClick(int x, int y, int button);
  Slot power$creativePaintSlot(int x, int y);
  Slot power$defaultCreativePaintSlot(int x, int y);
  ItemStack power$creativePaintPreview(Slot slot, ItemStack original);
  void power$cancelCreativePaint();
}
