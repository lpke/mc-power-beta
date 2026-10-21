package local.luke.power.inventory.tools.mixin;

import net.minecraft.class_320;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_320.class)
public interface SortingDoubleChestAccessor {
  @Accessor("field_1216") Inventory power$sortingFirst();
  @Accessor("field_1217") Inventory power$sortingSecond();
}
