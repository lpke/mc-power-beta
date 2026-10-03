package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import net.minecraft.world.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerGroupSizeMixin {

    @ModifyConstant(
            method = "tick",
            constant = @Constant(intValue = 4)
    )
    private static final int quickAdditions_spawnAttempts(int spawnAttempts) {
        return Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_GROUP_SIZE;
    }
}
