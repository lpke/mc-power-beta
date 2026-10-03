package local.luke.power.controls.mixin.tweaks.moresounds;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepEntity.class)
public class SheepEntityMixin extends AnimalEntity {
    public SheepEntityMixin(World arg) {
        super(arg);
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/Entity;)V"))
    public void snipSoundOnShear(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (ControlFeatures.FEATURES_CONFIG.shearingSounds) {
            player.world.playSound(player, "power_controls:entity.sheep.shear", 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }
    }
}
