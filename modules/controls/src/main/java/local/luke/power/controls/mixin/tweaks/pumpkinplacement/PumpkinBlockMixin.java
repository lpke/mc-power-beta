package local.luke.power.controls.mixin.tweaks.pumpkinplacement;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.block.Block;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin extends Block {
    public PumpkinBlockMixin(int id, Material material) {
        super(id, material);
    }

    @Inject(
            method = "canPlaceAt",
            at = @At("RETURN"),
            cancellable = true
    )
    private void annoyanceFix_canPlaceAt(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (ControlFeatures.TWEAKS_CONFIG.pumpkinsPlaceableLikeNormal) {
            cir.setReturnValue(super.canPlaceAt(world, x, y, z));
        }
    }
}
