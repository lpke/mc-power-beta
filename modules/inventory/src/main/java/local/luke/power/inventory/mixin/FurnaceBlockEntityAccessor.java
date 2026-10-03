package local.luke.power.inventory.mixin;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FurnaceBlockEntity.class)
public interface FurnaceBlockEntityAccessor {

    @Invoker(value = "getFuelTime")
    public int powerInventory_getFuelTime(ItemStack itemStack);
}
