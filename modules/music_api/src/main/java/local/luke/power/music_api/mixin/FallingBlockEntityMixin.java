package local.luke.power.music_api.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FallingBlockEntity.class, priority = 800)
public abstract class FallingBlockEntityMixin extends Entity {
    public FallingBlockEntityMixin(World world) {
        super(world);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/FallingBlockEntity;dropItem(II)Lnet/minecraft/entity/ItemEntity;"
            )
    )
    public ItemEntity powerMusic_dropItem(FallingBlockEntity instance, int id, int amount, Operation<ItemEntity> original) {
        return original.call(instance, Block.BLOCKS[id].asItem().id, amount);
    }
}
