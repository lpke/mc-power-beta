package com.github.telvarost.quickadditions.mixin.client;

import com.github.telvarost.quickadditions.Config;
import com.github.telvarost.zastavkaapi.ZastavkaHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public ClientPlayerEntity player;

    @Inject(
            method = "loadResource",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;loadMusic(Ljava/lang/String;Ljava/io/File;)V"
            ),
            cancellable = true
    )
    public void loadSoundFromDir(String file, File par2, CallbackInfo ci) {
        if (Config.config.MUSIC_CONFIG.disableDefaultMinecraftBGM) {
            System.out.println("Removing default Minecraft background music.");
            ci.cancel();
        }
    }

    @Inject(
            method = "changeDimension",
            at = @At("HEAD"),
            cancellable = true
    )
    public void quickAdditions_changeDimension(CallbackInfo ci) {
        if (Config.config.MUSIC_CONFIG.stopCurrentBgmOnPortalUse) {
            ZastavkaHelper.cancelCurrentBGM = true;
        } else if (Config.config.MUSIC_CONFIG.stopDimensionSpecificSongOnPortalUse) {
            if (ZastavkaHelper.songLevelId == this.player.dimensionId) {
                ZastavkaHelper.cancelCurrentBGM = true;
            }
        }
    }
}
