package local.luke.power.controls.mixin.tweaks.shearharvesting;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.block.Block;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

@Mixin(CobwebBlock.class)
public class CobwebBlockMixin extends Block {
    public CobwebBlockMixin(int id, Material material) {
        super(id, material);
    }

    // TODO: Convert to mixin inheritance
    @Override
    public int getDroppedItemCount(Random random) {
        if (ControlFeatures.TWEAKS_CONFIG.shearHarvesting) {
            return 0;
        }
        return super.getDroppedItemCount(random);
    }
}
