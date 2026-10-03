package local.luke.power.controls.mixin.tweaks.itemsdontstopminecart;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartEntity.class)
public class MinecartEntityMixin {
    @Inject(method = "getCollisionAgainstShape", at = @At("HEAD"), cancellable = true)
    public void preventCollisionWithItems(Entity other, CallbackInfoReturnable<Box> cir) {
        if (ControlFeatures.TWEAKS_CONFIG.preventItemsStoppingMinecarts) {
            if (other instanceof ItemEntity || other instanceof ArrowEntity) {
                cir.setReturnValue(null);
            }
        }
    }
}
