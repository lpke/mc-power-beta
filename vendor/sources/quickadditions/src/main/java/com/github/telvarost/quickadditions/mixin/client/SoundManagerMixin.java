package com.github.telvarost.quickadditions.mixin.client;

import com.github.telvarost.quickadditions.Config;
import com.github.telvarost.quickadditions.ModHelper;
import com.github.telvarost.zastavkaapi.ZastavkaHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow private int timeUntilNextSong;

    @Shadow private Random random;

    @Shadow public abstract void loadMusic(String string, File file);

    @Shadow public abstract void loadStreaming(String string, File file);

    @Shadow private GameOptions gameOptions;
    @Unique private int dimensionId = 0;

    @Unique private String biomeTag = "-unknown-";

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    public void SoundHelper(CallbackInfo ci) {
        File mainMenuThemeDir = new File("main-menu-theme");
        if (mainMenuThemeDir.exists()) {
            if (!mainMenuThemeDir.isDirectory()) {
                mainMenuThemeDir.delete();
                mainMenuThemeDir.mkdirs();
            } else {
                System.out.println("Looking for main menu songs in: main-menu-theme/");
                ModHelper.ModHelperFields.musicForMainMenu = new ArrayList<String>();

                File [] oggFiles = mainMenuThemeDir.listFiles(f-> f.toString().endsWith(".ogg"));
                if (null != oggFiles) {
                    for (int fileIndex = 0; fileIndex < oggFiles.length; fileIndex++) {
                        String songFileName = oggFiles[fileIndex].getName();
                        loadStreaming(songFileName, oggFiles[fileIndex]);
                        ModHelper.ModHelperFields.musicForMainMenu.add(songFileName.substring(0, songFileName.length() - 4));
                        System.out.println("Added: " + songFileName);
                    }
                }

                File [] musFiles = mainMenuThemeDir.listFiles(f-> f.toString().endsWith(".mus"));
                if (null != musFiles) {
                    for (int fileIndex = 0; fileIndex < musFiles.length; fileIndex++) {
                        String songFileName = musFiles[fileIndex].getName();
                        loadStreaming(songFileName, musFiles[fileIndex]);
                        ModHelper.ModHelperFields.musicForMainMenu.add(songFileName.substring(0, songFileName.length() - 4));
                        System.out.println("Added: " + songFileName);
                    }
                }

                File [] wavFiles = mainMenuThemeDir.listFiles(f-> f.toString().endsWith(".wav"));
                if (null != wavFiles) {
                    for (int fileIndex = 0; fileIndex < wavFiles.length; fileIndex++) {
                        String songFileName = wavFiles[fileIndex].getName();
                        loadStreaming(songFileName, wavFiles[fileIndex]);
                        ModHelper.ModHelperFields.musicForMainMenu.add(songFileName.substring(0, songFileName.length() - 4));
                        System.out.println("Added: " + songFileName);
                    }
                }
            }
        } else {
            mainMenuThemeDir.mkdirs();
        }

        File customMusicDir = new File("custom-music");
        if (customMusicDir.exists()) {
            if (!customMusicDir.isDirectory()) {
                customMusicDir.delete();
                customMusicDir.mkdirs();
            } else {
                System.out.println("Looking for songs in: custom-music/");

                File [] oggFiles = customMusicDir.listFiles(f-> f.toString().endsWith(".ogg"));
                if (null != oggFiles) {
                    for (int fileIndex = 0; fileIndex < oggFiles.length; fileIndex++) {
                        System.out.println("Added: " + oggFiles[fileIndex].getName());
                        loadMusic(oggFiles[fileIndex].getName(), oggFiles[fileIndex]);
                    }
                }

                File [] musFiles = customMusicDir.listFiles(f-> f.toString().endsWith(".mus"));
                if (null != musFiles) {
                    for (int fileIndex = 0; fileIndex < musFiles.length; fileIndex++) {
                        System.out.println("Added: " + musFiles[fileIndex].getName());
                        loadMusic(musFiles[fileIndex].getName(), musFiles[fileIndex]);
                    }
                }

                File [] wavFiles = customMusicDir.listFiles(f-> f.toString().endsWith(".wav"));
                if (null != wavFiles) {
                    for (int fileIndex = 0; fileIndex < wavFiles.length; fileIndex++) {
                        System.out.println("Added: " + wavFiles[fileIndex].getName());
                        loadMusic(wavFiles[fileIndex].getName(), wavFiles[fileIndex]);
                    }
                }
            }
        } else {
            customMusicDir.mkdirs();
        }

        this.timeUntilNextSong = this.random.nextInt(Config.config.MUSIC_CONFIG.musicCoundownRandomIntervalMax);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lpaulscode/sound/SoundSystem;playing(Ljava/lang/String;)Z",
                    ordinal = 0
            )
    )
    public boolean quickAdditions_tickDebugDisplay(SoundSystem instance, String string, Operation<Boolean> original) {
        if (gameOptions.debugHud) {
            if (instance.playing("BgMusic")) {
                ModHelper.ModHelperFields.currentBGM = ZastavkaHelper.currentMusicSong;
            } else if (instance.playing("streaming")) {
                ModHelper.ModHelperFields.currentBGM = ZastavkaHelper.currentStreamingSong;
            } else {
                ModHelper.ModHelperFields.currentBGM = "none";
            }
        }

        if (Config.config.MUSIC_CONFIG.disableBackgroundMusic) {
            return true;
        } else {
            return original.call(instance, string);
        }
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lpaulscode/sound/SoundSystem;backgroundMusic(Ljava/lang/String;Ljava/net/URL;Ljava/lang/String;Z)V"
            )
    )
    public void quickAdditions_tickChangeBackgroundMusicInterval(SoundSystem instance, String string, URL uRL, String string2, boolean bl, Operation<Void> original) {
        this.timeUntilNextSong = this.random.nextInt(Config.config.MUSIC_CONFIG.musicCoundownRandomIntervalMax) + Config.config.MUSIC_CONFIG.musicCoundownRandomIntervalMin;
        original.call(instance, string, uRL, string2, bl);
    }

    @WrapOperation(
            method = "playSound(Ljava/lang/String;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"
            )
    )
    public void quickAdditions_playSoundPositional(
            SoundSystem instance, String f, float v, Operation<Void> original,
            String id, float volume, float pitch
    ) {
        float adjustedVolume = v;

        if (id.startsWith("portal.portal")) {
            adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeNetherPortalAmbient;
        } else if (id.startsWith("ambient")) {
            if (id.startsWith("ambient.weather.rain")) {
                adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeRainAmbient;
            } else if (id.startsWith("ambient.cave.cave")) {
                adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeCaveAmbient;
            }
        } else if (id.startsWith("mob.ghast.moan")) {
            adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeGhastAmbient;
        }

        original.call(instance, f, adjustedVolume);
    }

    @WrapOperation(
            method = "playSound(Ljava/lang/String;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"
            )
    )
    public void quickAdditions_playSound(
            SoundSystem instance, String f, float v, Operation<Void> original,
            String id, float x, float y, float z, float volume, float pitch
    ) {
        float adjustedVolume = v;

        if (id.startsWith("portal.portal")) {
            adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeNetherPortalAmbient;
        } else if (id.startsWith("ambient")) {
            if (id.startsWith("ambient.weather.rain")) {
                adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeRainAmbient;
            } else if (id.startsWith("ambient.cave.cave")) {
                adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeCaveAmbient;
            }
        } else if (id.startsWith("mob.ghast.moan")) {
            adjustedVolume = v * Config.config.MUSIC_CONFIG.volumeGhastAmbient;
        }

        original.call(instance, f, adjustedVolume);
    }
}
