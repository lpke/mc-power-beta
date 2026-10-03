package local.luke.power.controls.mixin.tweaks.disabletallgrass;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.GrassPatchFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(GrassPatchFeature.class)
public class GrassPatchFeatureMixin {
    @Inject(method = "generate", at = @At(value = "HEAD"), cancellable = true)
    public void disableGeneration(World random, Random i, int j, int k, int par5, CallbackInfoReturnable<Boolean> cir) {
        if (ControlFeatures.OLD_FEATURES_CONFIG.disableTallGrassGeneration) {
            cir.cancel();
        }
    }
}
