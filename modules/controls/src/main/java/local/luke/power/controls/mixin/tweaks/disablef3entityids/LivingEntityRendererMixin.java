package local.luke.power.controls.mixin.tweaks.disablef3entityids;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "renderNameTag(Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
    public void disableDebugEntityIds(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        if (ControlFeatures.USER_INTERFACE_CONFIG.disableDebugEntityIdTags) {
            ci.cancel();
        }
    }
}
