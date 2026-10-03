package local.luke.power.controls.mixin.bugfixes.armoriconsfix;

import local.luke.power.controls.mixininterface.ControlFeaturesArmorSlot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.class_277$1")
public class ArmorSlotMixin implements ControlFeaturesArmorSlot {
    @Override
    public boolean powerControls$isArmorSlot() {
        return true;
    }
}
