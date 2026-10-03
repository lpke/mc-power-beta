package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import local.luke.power.mechanics.ModHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MonsterEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public class CreeperMixin extends MonsterEntity {

    public CreeperMixin(World arg) {
        super(arg);
        this.texture = "/mob/creeper.png";
    }

    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/mob/CreeperEntity;isCharged()Z"
            )
    )
    public void powerMechanics_tryAttackBeforeCreateExplosion(Entity f, float par2, CallbackInfo ci) {
        if (Config.config.EXPLOSION_AND_FIRE_CONFIG.disableCreeperExplosionBreakingBlocks)
        {
            ModHelper.ModHelperFields.cancelDestroyBlocks++;
            ModHelper.ModHelperFields.cancelDestroyBlocksPacket++;
        }
    }

}
