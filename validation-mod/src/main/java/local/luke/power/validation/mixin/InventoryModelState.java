package local.luke.power.validation.mixin;

import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_585.class)
public interface InventoryModelState {
  @Accessor("field_2523") float power$modelX();
  @Accessor("field_2524") float power$modelY();
}
