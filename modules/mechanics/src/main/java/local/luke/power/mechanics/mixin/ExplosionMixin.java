package local.luke.power.mechanics.mixin;

import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow private World world;

    @ModifyConstant(method = "playExplosionSound", constant = @Constant(floatValue = 0.3F))
    private float power$blockDropChance(float chance) {
        return world.isRemote ? chance : local.luke.power.rules.WorldRules.of(world.getProperties()).number(local.luke.power.rules.GameRule.EXPLOSION_DROP_CHANCE) / 100f;
    }
}
