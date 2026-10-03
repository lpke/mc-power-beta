package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Mutable
    @Shadow @Final public static Biome SAVANNA;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            )
    )
    protected boolean quickAdditions_biomeSpawnSpider(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableSpiderEntitySpawning) {
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
    protected boolean quickAdditions_biomeSpawnZombie(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableZombieEntitySpawning) {
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
                    ordinal = 2
            )
    )
    protected boolean quickAdditions_biomeSpawnSkeleton(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableSkeletonEntitySpawning) {
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
                    ordinal = 3
            )
    )
    protected boolean quickAdditions_biomeSpawnCreeper(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableCreeperEntitySpawning) {
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
                    ordinal = 4
            )
    )
    protected boolean quickAdditions_biomeSpawnSlime(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableSlimeEntitySpawning) {
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
                    ordinal = 5
            )
    )
    protected boolean quickAdditions_biomeSpawnSheep(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableSheepEntitySpawning) {
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
                    ordinal = 6
            )
    )
    protected boolean quickAdditions_biomeSpawnPig(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enablePigEntitySpawning) {
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
                    ordinal = 7
            )
    )
    protected boolean quickAdditions_biomeSpawnChicken(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableChickenEntitySpawning) {
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
                    ordinal = 8
            )
    )
    protected boolean quickAdditions_biomeSpawnCow(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableCowEntitySpawning) {
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
                    ordinal = 9
            )
    )
    protected boolean quickAdditions_biomeSpawnSquid(List instance, Object entitySpawnGroup, Operation<Boolean> original) {
        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.SPAWN_LIST_CONFIG.enableSquidEntitySpawning) {
            return original.call(instance, entitySpawnGroup);
        } else {
            return true;
        }
    }

    @Inject(method = "init", at = @At("RETURN"))
    private static void quickAdditions_init(CallbackInfo ci) {
        if (Config.config.WEATHER_CONFIG.disableRainInSavannaBiomes) {
            SAVANNA.disableRain();
        }
    }
}
