package local.luke.power.controls.mixin.tweaks.beta18leavesrender;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Apply after StationAPI's state-brightness argument modifier. Ordinary samples
// retain that argument; the legacy leaf option retains its own fixed emission.
@Mixin(value = Block.class, priority = 900)
public class BlockMixin {
    @Shadow @Final public static int[] BLOCKS_LIGHT_LUMINANCE;
    @Shadow @Final public int id;

    // A cancellable HEAD callback allocates
    // for every block-light sample when the JIT cannot eliminate its wrapper.
    @Redirect(method = "getLuminance", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/BlockView;getNaturalBrightness(IIII)F"))
    private float changeLuminance(BlockView blockView, int x, int y, int z, int emission) {
        if (ControlFeatures.TWEAKS_CONFIG.beta18LeavesRendering
                && blockView.getMaterial(x, y, z) == Material.LEAVES)
            return Math.min(blockView.getNaturalBrightness(x, y, z, BLOCKS_LIGHT_LUMINANCE[this.id]), 0.2F);
        return blockView.getNaturalBrightness(x, y, z, emission);
    }
}
