package com.github.telvarost.zastavkaapi.mixin.client;

import com.github.telvarost.zastavkaapi.ZastavkaHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.biome.Biome;
import net.modificationstation.stationapi.api.entity.player.PlayerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public abstract class SoundHelperMixin {

    @Shadow private static SoundSystem soundSystem;
    @Shadow private GameOptions gameOptions;
    @Shadow private static boolean started;

    @Unique private int dimensionId = 0;
    @Unique private String biomeTag = "-unknown-";

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void zastavkaApi_tickCancelSong(CallbackInfo ci) {
        if (ZastavkaHelper.cancelCurrentBGM) {
            ZastavkaHelper.cancelCurrentBGM = false;

            if (started && this.gameOptions.musicVolume != 0.0F) {
                soundSystem.stop("BgMusic");
            }
        }
    }

    @WrapOperation(
            method = "playStreaming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundEntry;get(Ljava/lang/String;)Lnet/minecraft/client/sound/Sound;"
            )
    )
    public Sound zastavkaApi_tickGetStreamingSong(SoundEntry instance, String string, Operation<Sound> original) {
        Sound streamingSong = original.call(instance, string);

        if (null != streamingSong)
        {
            ZastavkaHelper.currentStreamingSong = streamingSong.id;
        }

        return streamingSong;
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundEntry;getSounds()Lnet/minecraft/client/sound/Sound;"
            )
    )
    public Sound zastavkaApi_tickGetMusicSong(SoundEntry instance, Operation<Sound> original) {
        Sound currentMusic = original.call(instance);
        ZastavkaHelper.currentMusicSong = currentMusic.id;

        if (  (null != currentMusic)
           && (null != currentMusic.id)
           && (currentMusic.id.contains("-specific."))
        ) {
            PlayerEntity player = PlayerHelper.getPlayerFromGame();
            if (null != player) {
                dimensionId = player.dimensionId;
            }

            if (currentMusic.id.contains("-level" + dimensionId + "-")) {
                ZastavkaHelper.songLevelId = dimensionId;
                return currentMusic;
            }

            if (0 == dimensionId) {
                if (currentMusic.id.contains("-overworld-")) {
                    ZastavkaHelper.songLevelId = dimensionId;
                    return currentMusic;
                }
            } else if (-1 == dimensionId) {
                if (currentMusic.id.contains("-nether-")) {
                    ZastavkaHelper.songLevelId = dimensionId;
                    return currentMusic;
                }
            }

            if (null != player) {
                if (null != player.world) {
                    if (null != player.world.method_1781()) {
                        Biome biome = player.world.method_1781().getBiome((int) Math.floor(player.x), (int) Math.floor(player.z));
                        if (null != biome && null != biome.name) {
                            biomeTag = '-' + biome.name.toLowerCase() + '-';
                        }
                    }
                }
            }

            if (currentMusic.id.contains(biomeTag)) {
                ZastavkaHelper.songLevelId = Integer.MAX_VALUE;
                return currentMusic;
            }

            System.out.println("Skipping: " + currentMusic.id);
            ZastavkaHelper.songLevelId = Integer.MAX_VALUE;
            return null;
        } else if (  (null != currentMusic)
                  && (null != currentMusic.soundFile)
                  && (currentMusic.soundFile.toString().contains("-specific."))
        ) {
            PlayerEntity player = PlayerHelper.getPlayerFromGame();
            if (null != player) {
                dimensionId = player.dimensionId;
            }

            if (currentMusic.soundFile.toString().contains("-level" + dimensionId + "-")) {
                ZastavkaHelper.songLevelId = dimensionId;
                return currentMusic;
            }

            if (0 == dimensionId) {
                if (currentMusic.soundFile.toString().contains("-overworld-")) {
                    ZastavkaHelper.songLevelId = dimensionId;
                    return currentMusic;
                }
            } else if (-1 == dimensionId) {
                if (currentMusic.soundFile.toString().contains("-nether-")) {
                    ZastavkaHelper.songLevelId = dimensionId;
                    return currentMusic;
                }
            }

            if (null != player) {
                if (null != player.world) {
                    if (null != player.world.method_1781()) {
                        Biome biome = player.world.method_1781().getBiome((int) Math.floor(player.x), (int) Math.floor(player.z));
                        if (null != biome && null != biome.name) {
                            biomeTag = '-' + biome.name.toLowerCase() + '-';
                        }
                    }
                }
            }

            if (currentMusic.soundFile.toString().contains(biomeTag)) {
                ZastavkaHelper.songLevelId = Integer.MAX_VALUE;
                return currentMusic;
            }

            System.out.println("Skipping: " + currentMusic.soundFile);
            ZastavkaHelper.songLevelId = Integer.MAX_VALUE;
            return null;
        }

        ZastavkaHelper.songLevelId = Integer.MAX_VALUE;
        return currentMusic;
    }
}
