package local.luke.power.controls.mixin.tweaks.biggerchickenhitbox;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChickenEntity.class)
public class ChickenEntityMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 0.3F))
    public float modifyWidth(float constant) {
        if (ControlFeatures.TWEAKS_CONFIG.expandChickenHitbox) {
            return 0.4F;
        }
        return constant;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 0.4F))
    public float modifyHeight(float constant) {
        if (ControlFeatures.TWEAKS_CONFIG.expandChickenHitbox) {
            return 0.7F;
        }
        return constant;
    }
}
