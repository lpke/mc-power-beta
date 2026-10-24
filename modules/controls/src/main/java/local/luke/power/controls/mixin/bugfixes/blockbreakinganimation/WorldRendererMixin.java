package local.luke.power.controls.mixin.bugfixes.blockbreakinganimation;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @ModifyArg(method = "renderMiningProgress", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glPolygonOffset(FF)V", ordinal = 0, remap = false), index = 1)
    private float increaseBreakingAnimationZOffset(float original) {
        // Offset only fragment depth. Scaling this camera-relative draw detaches cracks from blocks.
        return ControlFeatures.BUGFIXES_CONFIG.breakingAnimationFix ? -10F : original;
    }
}
