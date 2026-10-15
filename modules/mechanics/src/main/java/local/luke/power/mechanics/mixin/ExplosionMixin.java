package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow private World world;

    @ModifyConstant(method = "playExplosionSound", constant = @Constant(floatValue = 0.3F))
    private float power$blockDropChance(float chance) {
        return world.isRemote ? chance : Config.config.EXPLOSION_AND_FIRE_CONFIG.chanceBlocksDropWhenExploded;
    }
}
