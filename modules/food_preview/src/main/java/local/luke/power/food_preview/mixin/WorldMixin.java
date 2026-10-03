package local.luke.power.food_preview.mixin;

import local.luke.power.food_preview.FoodPreview;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={World.class})
public class WorldMixin {
    @Inject(method={"method_242"}, at={@At(value="HEAD")})
    public void tickPulse(CallbackInfo ci) {
        if ((FoodPreview.pulseUnclamped += (float)FoodPreview.pulseDir * 0.125f) >= 1.5f) {
            FoodPreview.pulseDir = -1;
        }
        if (FoodPreview.pulseUnclamped <= -0.5f) {
            FoodPreview.pulseDir = 1;
        }
        FoodPreview.pulseValue = this.clamp(FoodPreview.pulseUnclamped, 0.0f, 1.0f) * 0.65f;
    }

    @Unique
    private float clamp(float value, float min, float max) {
        if (value > max) {
            return max;
        }
        return Math.max(value, min);
    }
}

