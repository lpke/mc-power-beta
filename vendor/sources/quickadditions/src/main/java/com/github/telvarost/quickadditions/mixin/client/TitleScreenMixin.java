package com.github.telvarost.quickadditions.mixin.client;

import com.github.telvarost.quickadditions.Config;
import com.github.telvarost.quickadditions.ModHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

import java.util.Random;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    @Shadow private float ticks;
    @Unique private boolean startedMainMenuSong = false;

    @Inject(
            method = "init",
            at = @At("RETURN"),
            cancellable = true
    )
    public void init(CallbackInfo ci) {
        if (ticks <= 0 || !startedMainMenuSong) {
            if (Config.config.MUSIC_CONFIG.mainMenuThemeEnabled) {
                playMainMenuTheme();
            }
        } else {
            SoundSystem soundSystem = ((SoundManagerAccessor)minecraft.soundManager).getSoundSystem();
            boolean started = ((SoundManagerAccessor)minecraft.soundManager).getStarted();
            if (started && soundSystem.playing("streaming")) {
                soundSystem.setVolume("streaming", minecraft.options.musicVolume);
            }
        }
    }

    @Unique
    private void playMainMenuTheme() {
        SoundSystem soundSystem = ((SoundManagerAccessor)minecraft.soundManager).getSoundSystem();
        SoundEntry streaming = ((SoundManagerAccessor)minecraft.soundManager).getStreaming();
        boolean started = ((SoundManagerAccessor)minecraft.soundManager).getStarted();
        if (started && minecraft.options.musicVolume != 0.0F) {
            if (  (null != ModHelper.ModHelperFields.musicForMainMenu)
               && (!ModHelper.ModHelperFields.musicForMainMenu.isEmpty())
            ) {
                Random seedRand = new Random();
                Random rand = new Random(System.currentTimeMillis() + seedRand.nextInt());
                int selectedMainMenuTheme = rand.nextInt(ModHelper.ModHelperFields.musicForMainMenu.size());

                Sound sound = streaming.get(ModHelper.ModHelperFields.musicForMainMenu.get(selectedMainMenuTheme));
                if (sound != null) {
                    if (Config.config.MUSIC_CONFIG.mainMenuThemeOverridesBGM) {
                        if (soundSystem.playing("BgMusic")) {
                            soundSystem.stop("BgMusic");
                        }
                    }

                    if (!soundSystem.playing("BgMusic") || Config.config.MUSIC_CONFIG.mainMenuThemeOverridesBGM) {
                        if (!soundSystem.playing("streaming")) {
                            soundSystem.backgroundMusic("streaming", sound.soundFile, sound.id, false);
                            soundSystem.setVolume("streaming", minecraft.options.musicVolume);
                            soundSystem.play("streaming");
                            startedMainMenuSong = true;
                        } else {
                            soundSystem.setVolume("streaming", minecraft.options.musicVolume);
                        }
                    }
                }
            }
        }
    }
}
