package local.luke.power.controls.mixin.tweaks.stepassist;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends EntityMixin {
    @Override
    public float injectChangeStepHeight(float stepHeight) {
        if (ControlFeatures.FEATURES_CONFIG.stepAssist) {
            stepHeight += (this.isSneaking() ? 0.0F : 0.5F);
        }
        
        return stepHeight;
    }
}
