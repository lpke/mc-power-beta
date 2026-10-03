package com.github.telvarost.quickadditions.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import paulscode.sound.SoundSystem;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {

    /** - Getters */
    @Accessor("soundSystem")
    SoundSystem getSoundSystem();
    @Accessor("streamingSounds")
    SoundEntry getStreaming();
    @Accessor("started")
    boolean getStarted();

    /** - Setters */
    @Accessor("timeUntilNextSong")
    public void setMusicCountdown(int musicCountdown);
}
