package local.luke.power.controls.mixin.tweaks.stepassist;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends EntityMixin {
    @Override
    public float injectChangeStepHeight(Entity instance, Operation<Float> original) {
        Float stepHeight = original.call(instance);
        
        if (ControlFeatures.FEATURES_CONFIG.stepAssist) {
            stepHeight += (this.isSneaking() ? 0.0F : 0.5F);
        }
        
        return stepHeight;
    }
}
