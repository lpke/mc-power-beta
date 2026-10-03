package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.ForestBiome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ForestBiome.class)
public class ForestBiomeMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    protected boolean quickAdditions_forestBiomeSpawnWolf(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableWolfEntitySpawning) {
            return original.call(instance, entitySpawnGroup);
        } else {
            return true;
        }
    }
}
