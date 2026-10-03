package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.HellBiome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(HellBiome.class)
public class HellBiomeMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    protected boolean quickAdditions_hellBiomeSpawnGhast(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableGhastEntitySpawning) {
            return original.call(instance, entitySpawnGroup);
        } else {
            return true;
        }
    }

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 1
            )
    )
    protected boolean quickAdditions_hellBiomeSpawnZombiePigman(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableZombiePigmanEntitySpawning) {
            return original.call(instance, entitySpawnGroup);
        } else {
            return true;
        }
    }
}
