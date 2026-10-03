package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.SkyBiome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SkyBiome.class)
public class SkyBiomeMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    protected boolean quickAdditions_skyBiomeSpawnChicken(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableChickenEntitySpawning) {
            return original.call(instance, entitySpawnGroup);
        } else {
            return true;
        }
    }
}
