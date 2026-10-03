package local.luke.power.controls.mixin.tweaks.cloudheight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @WrapOperation(method = "renderClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;getCloudHeight()F"))
    public float changeCloudHeight(Dimension instance, Operation<Float> original) {
        return ModOptions.getCloudHeight();
    }

    @WrapOperation(method = "renderFancyClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;getCloudHeight()F"))
    public float ChangeFancyCloudHeight(Dimension instance, Operation<Float> original) {
        return ModOptions.getCloudHeight();
    }
}
