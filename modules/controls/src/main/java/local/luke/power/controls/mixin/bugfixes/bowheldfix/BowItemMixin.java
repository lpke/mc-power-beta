package local.luke.power.controls.mixin.bugfixes.bowheldfix;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin extends ItemMixin {
    @Override
    protected void power_controls$isHandheld(CallbackInfoReturnable<Boolean> cir) {
        if (ControlFeatures.BUGFIXES_CONFIG.bowHeldFix) {
            cir.setReturnValue(true);
        }
    }
}
