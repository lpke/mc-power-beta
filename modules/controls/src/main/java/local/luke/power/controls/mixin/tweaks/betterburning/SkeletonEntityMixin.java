package local.luke.power.controls.mixin.tweaks.betterburning;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkeletonEntity.class)
public abstract class SkeletonEntityMixin extends Entity {

    public SkeletonEntityMixin(World world) {
        super(world);
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean createBurningArrow(World world, Entity arrowEntity, Operation<Boolean> original) {
        if (ControlFeatures.FEATURES_CONFIG.betterBurning.enableBetterBurning && ControlFeatures.FEATURES_CONFIG.betterBurning.skeletonsBurningArrows && this.fireTicks > 0) {
            if (this.random.nextInt(0, 100) < ControlFeatures.FEATURES_CONFIG.betterBurning.skeletonBurningArrowChance) {
                arrowEntity.fireTicks = 400;
            }
        }
        return original.call(world, arrowEntity);
    }
}
