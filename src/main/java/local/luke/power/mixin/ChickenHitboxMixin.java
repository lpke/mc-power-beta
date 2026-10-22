package local.luke.power.mixin;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.class_313;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Constructor-only spacing changes leave existing chickens and their boxes stale. */
@Mixin(class_313.class)
public abstract class ChickenHitboxMixin {
  @Inject(method = "method_937", at = @At("HEAD"))
  private void power$refreshHitbox(CallbackInfo ci) {
    Entity chicken = (Entity)(Object)this;
    boolean expanded = ControlFeatures.TWEAKS_CONFIG.expandChickenHitbox;
    float width = expanded ? .4F : .3F, height = expanded ? .7F : .4F;
    if (chicken.spacingXZ == width && chicken.spacingY == height
        && Math.abs(chicken.boundingBox.maxX - chicken.boundingBox.minX - width) < .00001
        && Math.abs(chicken.boundingBox.maxY - chicken.boundingBox.minY - height) < .00001) return;
    chicken.spacingXZ = width; chicken.spacingY = height;
    chicken.method_1340(chicken.x, chicken.y, chicken.z);
  }
}
