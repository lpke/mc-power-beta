package com.github.telvarost.quickadditions.mixin;

import com.github.telvarost.quickadditions.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow private boolean allowMonsterSpawning;

    @Shadow public int difficulty;

    @Shadow private boolean allPlayersSleeping;

    @Shadow public List players;

    @Shadow public boolean isRemote;

    @Shadow protected WorldProperties properties;

    @Shadow protected abstract void afterSkipNight();

    @Shadow public abstract boolean isRaining();

    @Shadow public abstract boolean isThundering();

    @Shadow protected abstract void clearWeather();

    @Shadow public Random random;

    @Shadow protected ChunkSource chunkSource;

    @Shadow
    public abstract boolean processScheduledTicks(boolean flush);

    @Shadow
    protected abstract void manageChunkUpdatesAndEvents();

    @Shadow
    public abstract void tickEntities();

    @Unique private int highestBlockYLocation = 0;
    @Unique private BoatEntity skipObject;

    @Inject(
            method = "canSkipNight",
            at = @At("HEAD"),
            cancellable = true
    )
    public void quickAdditions_canSkipNight(CallbackInfoReturnable<Boolean> cir) {
        if (1.0f > Config.config.SLEEP_CONFIG.asleepPlayerPercentageForSkippingNight) {
            if (this.allPlayersSleeping && !this.isRemote) {
                int playersFullySleepingCount = 0;

                for (Object var2 : this.players) {
                    if (((PlayerEntity)var2).isFullyAsleep()) {
                        playersFullySleepingCount++;
                    }
                }

                if (  ( null != this.players )
                   && ( !this.players.isEmpty() )
                   && ( 0 < playersFullySleepingCount )
                   && ( ((float) playersFullySleepingCount / this.players.size()) >= Config.config.SLEEP_CONFIG.asleepPlayerPercentageForSkippingNight )
                ) {
                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(false);
                }
            } else {
                cir.setReturnValue(false);
            }
        }
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;canSkipNight()Z"
            )
    )
    public boolean quickAdditions_tickCanSkipNight(World instance, Operation<Boolean> original) {
        boolean skippingNight;

        if (Config.config.SLEEP_CONFIG.bedsSpeedUpNightRatherThanSkipIt) {
            skippingNight = original.call(instance);

            if (skippingNight) {
                boolean var1 = false;
                if (this.allowMonsterSpawning && this.difficulty >= 1) {
                    var1 = NaturalSpawner.spawnMonstersAndWakePlayers(instance, this.players);
                }

                if (!var1) {
                    long currentTime = this.properties.getTime();
                    long desiredTime = (currentTime + 24000L) - currentTime % 24000L;

                    for (int chunkSourceTick = 1; chunkSourceTick < Config.config.SLEEP_CONFIG.bedsSpeedUpNightTickRate; chunkSourceTick ++)
                    {
                        if (Config.config.SLEEP_CONFIG.bedsSpeedUpNightTickWorld) {
                            this.tickEntities();
                            this.chunkSource.tick();
                        }

                        long var6 = this.properties.getTime() + 1L;
                        this.properties.setTime(var6);

                        if (Config.config.SLEEP_CONFIG.bedsSpeedUpNightTickWorld) {
                            this.processScheduledTicks(false);
                            this.manageChunkUpdatesAndEvents();
                        }
                    }

                    if (desiredTime < (currentTime + Config.config.SLEEP_CONFIG.bedsSpeedUpNightTickRate)) {
                        this.afterSkipNight();
                    }
                }
            }

            return false;
        } else {
            return original.call(instance);
        }
    }

    @Inject(
            method = "updateSleepingPlayers",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void quickAdditions_updateSleepingPlayers(CallbackInfo ci) {
        if (1.0f > Config.config.SLEEP_CONFIG.asleepPlayerPercentageForSkippingNight) {
            int playersSleepingCount = 0;

            for (Object var2 : this.players) {
                if (((PlayerEntity)var2).isSleeping()) {
                    playersSleepingCount++;
                }
            }

            if (  ( null != this.players )
               && ( !this.players.isEmpty() )
               && ( 0 < playersSleepingCount )
               && ( ((float) playersSleepingCount / this.players.size()) >= Config.config.SLEEP_CONFIG.asleepPlayerPercentageForSkippingNight)
            ) {
                this.allPlayersSleeping = true;
            } else {
                this.allPlayersSleeping = false;
            }

            ci.cancel();
        }
    }

    @WrapOperation(
            method = "afterSkipNight",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;clearWeather()V"
            )
    )
    protected void quickAdditions_afterSkipNight(World instance, Operation<Void> original) {
        if (Config.config.WEATHER_CONFIG.sleepOnlyResetsWeatherWhenRaining) {
            if (isRaining() || isThundering()) {
                this.clearWeather();
            } else {
                /** - Do nothing */
            }
        } else {
            this.clearWeather();
        }
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;getThunderTime()I"
            )
    )
    protected int quickAdditions_disableThunder(WorldProperties instance, Operation<Integer> original) {
        if (Config.config.WEATHER_CONFIG.disableThunder) {
            if (this.properties.getThundering()) {
                this.properties.setThundering(false);
            }
            return 12000;
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;getRainTime()I"
            )
    )
    protected int quickAdditions_disableRain(WorldProperties instance, Operation<Integer> original) {
        if (Config.config.WEATHER_CONFIG.disableRain) {
            if (this.properties.getRaining()) {
                this.properties.setRaining(false);
            }
            return 12000;
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;setThunderTime(I)V",
                    ordinal = 0
            )
    )
    protected void quickAdditions_setThunderDuration(WorldProperties instance, int thunderTime, Operation<Void> original) {
        original.call(instance, this.random.nextInt(Config.config.WEATHER_CONFIG.thunderDurationRandomLimit) + Config.config.WEATHER_CONFIG.thunderDurationMinimum);
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;setThunderTime(I)V",
                    ordinal = 1
            )
    )
    protected void quickAdditions_setTimeUntilThunder(WorldProperties instance, int thunderTime, Operation<Void> original) {
        original.call(instance, this.random.nextInt(Config.config.WEATHER_CONFIG.timeUntilThunderRandomLimit) + Config.config.WEATHER_CONFIG.timeUntilThunderMinimum);
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;setRainTime(I)V",
                    ordinal = 0
            )
    )
    protected void quickAdditions_setRainDuration(WorldProperties instance, int rainTime, Operation<Void> original) {
        original.call(instance, this.random.nextInt(Config.config.WEATHER_CONFIG.rainDurationRandomLimit) + Config.config.WEATHER_CONFIG.rainDurationMinimum);
    }

    @WrapOperation(
            method = "updateWeatherCycles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldProperties;setRainTime(I)V",
                    ordinal = 1
            )
    )
    protected void quickAdditions_setTimeUntilRain(WorldProperties instance, int rainTime, Operation<Void> original) {
        original.call(instance, this.random.nextInt(Config.config.WEATHER_CONFIG.timeUntilRainRandomLimit) + Config.config.WEATHER_CONFIG.timeUntilRainMinimum);
    }

    @WrapOperation(
            method = "manageChunkUpdatesAndEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getTopSolidBlockY(II)I"
            )
    )
    private int quickAdditions_tickGetTopSolidBlock(World instance, int x, int z, Operation<Integer> original) {
        highestBlockYLocation = original.call(instance, x, z);
        return highestBlockYLocation;
    }

    @WrapOperation(
            method = "manageChunkUpdatesAndEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canSnow()Z"
            )
    )
    private boolean quickAdditions_tickCanSnow(Biome instance, Operation<Boolean> original) {
        boolean allowSnow = original.call(instance);

        if (Config.config.WEATHER_CONFIG.enableAlwaysSnowAboveSetYLevel) {
            if (Config.config.WEATHER_CONFIG.alwaysSnowAboveThisYLevel < highestBlockYLocation) {
                allowSnow = true;
            }
        }

        return allowSnow;
    }

    @WrapOperation(
            method = "countEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;get(I)Ljava/lang/Object;"
            )
    )
    public Object quickAdditions_countEntities(List instance, int index, Operation<Object> original) {
        Object entityObject = original.call(instance, index);

        if (Config.config.ENTITY_SPAWN_MECHANICS_CONFIG.doNotCountSaddledPigs) {
            if (entityObject instanceof PigEntity) {
                PigEntity pigEntity = (PigEntity) entityObject;
                if (pigEntity.isSaddled()) {
                    if (null == skipObject) {
                        skipObject = new BoatEntity(pigEntity.world);
                    }
                    return skipObject;
                }
            }
        }

        return entityObject;
    }

}
