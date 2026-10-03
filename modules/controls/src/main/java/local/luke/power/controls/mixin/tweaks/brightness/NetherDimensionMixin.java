package local.luke.power.controls.mixin.tweaks.brightness;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.NetherDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("DuplicatedCode")
@Mixin(NetherDimension.class)
public class NetherDimensionMixin extends Dimension {
    @Inject(method = "initBrightnessTable", at = @At(value = "HEAD"), cancellable = true)
    public void initAdjustedBrightnessTable(CallbackInfo ci) {
        // Calculate brightness
        float brightness = ModOptions.brightness;
        float[] lightLevels = new float[16];
        float minimumLevel = 0.05F;

        if (this.isNether) {
            minimumLevel = 0.1F + brightness * 0.15F;
        }

        float k = 3.0f * (1.0F - brightness);
        for (int level = 0; level <= 15; ++level) {
            float var3 = 1.0F - (float) level / 15.0f;
            lightLevels[level] = (1.0F - var3) / (var3 * k + 1.0F) * (1.0F - minimumLevel) + minimumLevel;
        }

        // Write the light table
        this.lightLevelToLuminance = lightLevels;

        // Cancel to prevent the original calculation
        ci.cancel();
    }
}
