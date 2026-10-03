package local.luke.power.controls.mixin.tweaks.igniteentities;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.FlintAndSteel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FlintAndSteel.class)
public class FlintAndSteelItemMixin extends Item {
    public FlintAndSteelItemMixin(int id) {
        super(id);
    }

    // TODO: Convert to mixin inheritance
    @Override
    public void useOnEntity(ItemStack stack, LivingEntity entity) {
        if (ControlFeatures.TWEAKS_CONFIG.allowIgnitingEntities) {
            entity.fireTicks += 100;
            stack.damage(1, null);
        }
        
        super.useOnEntity(stack, entity);
    }
}
