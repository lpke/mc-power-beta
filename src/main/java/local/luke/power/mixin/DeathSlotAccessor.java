package local.luke.power.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Slot.equals(Inventory, int) exists only in the merged server API. */
@Mixin(Slot.class)
public interface DeathSlotAccessor {
  @Accessor("inventory") Inventory power$deathInventory();
  @Accessor("index") int power$deathIndex();
}
