package local.luke.power.recipes.mixin;

import java.util.List;
import net.minecraft.class_453;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_453.class)
public interface ShapelessRecipeAccessor {
  @Accessor("field_1720") List<ItemStack> power$recipeInputs();
}
