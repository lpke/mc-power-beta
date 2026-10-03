package local.luke.power.controls.mixin.bugfixes.blockeffectivenessfix;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.EffectiveBlocksLists;
import net.minecraft.block.Block;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public class AxeItemMixin extends ToolItemMixin {
    @Override
    protected void power_controls$getMiningSpeedMultiplier(ItemStack stack, Block block, CallbackInfoReturnable<Float> cir) {
        if (ControlFeatures.BUGFIXES_CONFIG.blockEffectivenessFix) {
            if (EffectiveBlocksLists.axeBlocks.contains(block)) {
                cir.setReturnValue(this.miningSpeed);
            }
        }
    }
}
