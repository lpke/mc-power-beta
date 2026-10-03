package local.luke.power.gold.mixin;

import local.luke.power.gold.Config;
import local.luke.power.gold.ModHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;
import net.minecraft.entity.mob.MonsterEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.World;

@Mixin(SkeletonEntity.class)
public class SkeletonMixin extends MonsterEntity {
    public SkeletonMixin(World arg) {
        super(arg);
    }

    @WrapOperation(
            method = "dropItems",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Random;nextInt(I)I"
            )
    )
    protected int getDrops(Random instance, int dropCount, Operation<Integer> original) {
        if (  (Config.config.enableGoldSwordLooting)
           && (0 < ModHelper.ModHelperFields.UsingGoldSword)
        ) {
            ModHelper.ModHelperFields.UsingGoldSword--;
            return original.call(instance, dropCount) + 1;
        } else {
            return original.call(instance, dropCount);
        }
    }
}
