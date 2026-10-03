package local.luke.power.recipes.mixin;

import local.luke.power.recipes.api.SubItemProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeavesBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(LeavesBlockItem.class)
public class LeavesBlockItemMixin {
    
    @Unique
    @SubItemProvider
    public List<ItemStack> getSubItems() {
        return List.of(
                new ItemStack(Item.class.cast(this), 1, 0),
                new ItemStack(Item.class.cast(this), 1, 1),
                new ItemStack(Item.class.cast(this), 1, 2)
        );
    }
}
