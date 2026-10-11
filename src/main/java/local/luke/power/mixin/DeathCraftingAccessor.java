package local.luke.power.mixin;

import net.minecraft.class_159;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Rollback must restore input stacks without invoking the failed recipe callback again. */
@Mixin(class_159.class)
public interface DeathCraftingAccessor {
  @Accessor("field_1132") ItemStack[] power$deathStacks();
}
