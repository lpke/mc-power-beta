package local.luke.power.controls.mixin.bugfixes.fishvelocityfix;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
    @ModifyConstant(method = "use", constant = @Constant(doubleValue = 0.08))
    public double reduceFishVerticalVelocity(double constant) {
        if (ControlFeatures.BUGFIXES_CONFIG.fishVelocityFix) {
            return 0.04;
        } else {
            return constant;
        }
    }
}
