package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PigEntity.class)
public abstract class PigEntityMixin extends AnimalEntity {
    @Shadow public abstract boolean isSaddled();

    public PigEntityMixin(World world) {
        super(world);
    }

    @Override
    protected boolean canDespawn() {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.disableLivingEntitiesDespawning) {
            return false;
        } else if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.disableSaddledPigsDespawning) {
            if (isSaddled()) {
                return false;
            }
        }

        return true;
    }
}
