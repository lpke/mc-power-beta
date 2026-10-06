package local.luke.power.autowalk.mixin;
import net.minecraft.class_320;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_320.class)
public interface DoubleInventoryAccessor {
  @Accessor("field_1216") Inventory power$first();
  @Accessor("field_1217") Inventory power$second();
}
