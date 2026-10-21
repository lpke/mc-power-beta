package local.luke.power.recipes.mixin;

import net.minecraft.class_193;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_193.class)
public interface ShapedRecipeAccessor {
  @Accessor("field_699") int power$recipeWidth();
  @Accessor("field_700") int power$recipeHeight();
  @Accessor("field_701") ItemStack[] power$recipeInputs();
}
