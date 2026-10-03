package local.luke.power.controls.mixin.tweaks.minecartboosters;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.vehicle.MinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecartEntity.class)
public class MinecartEntityMixin {
    @ModifyVariable(method = "onCollision", at = @At(value = "STORE"), ordinal = 6)
    private double betaTweaks_minecartBoosterCondition(double constant) {
        if (ControlFeatures.OLD_FEATURES_CONFIG.minecartBoosters) {
            return 0;
        } else {
            return constant;
        }
    }
}
