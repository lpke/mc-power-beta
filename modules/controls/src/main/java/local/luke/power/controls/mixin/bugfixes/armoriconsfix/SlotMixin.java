package local.luke.power.controls.mixin.bugfixes.armoriconsfix;

import local.luke.power.controls.mixininterface.ControlFeaturesArmorSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Slot.class)
public class SlotMixin implements ControlFeaturesArmorSlot {
    @Override
    public boolean powerControls$isArmorSlot() {
        return false;
    }
}
