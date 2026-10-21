package local.luke.power.recipes.mixin;

import net.minecraft.class_138;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_138.class)
public interface RecipeFurnaceAccessor {
  @Accessor("field_1569") ItemStack[] power$recipeStacks();
  @Invoker("method_1280") int power$recipeFuelTime(ItemStack stack);
}
