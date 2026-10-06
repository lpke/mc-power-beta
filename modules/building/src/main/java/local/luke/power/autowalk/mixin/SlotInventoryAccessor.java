package local.luke.power.autowalk.mixin;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotInventoryAccessor {
  @Accessor("inventory") Inventory power$inventory();
}
