package local.luke.power.compat.mixin.stackablebuckets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import local.luke.power.compat.BucketStacks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @WrapOperation(method = "use", at = @At(value = "NEW", target = "(Lnet/minecraft/item/Item;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack power$fillOneBucket(Item item, Operation<ItemStack> original,
            @Local(argsOnly = true) ItemStack input, @Local(argsOnly = true) PlayerEntity player) {
        return BucketStacks.fill(input, player, original.call(item));
    }
}
