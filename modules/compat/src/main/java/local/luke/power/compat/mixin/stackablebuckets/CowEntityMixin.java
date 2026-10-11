package local.luke.power.compat.mixin.stackablebuckets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import local.luke.power.compat.BucketStacks;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CowEntity.class)
public abstract class CowEntityMixin {
    @WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
    private void power$milkOneBucket(PlayerInventory inventory, int slot, ItemStack filled,
            Operation<Void> original, @Local(argsOnly = true) PlayerEntity player) {
        original.call(inventory, slot, BucketStacks.fill(inventory.getSelectedItem(), player, filled));
    }
}
