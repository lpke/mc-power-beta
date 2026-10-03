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


@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public ClientPlayerEntity player;

    // Soundtrack filtering is owned by the live audio settings. Keep the pool
    // loaded so disabling and re-enabling the soundtrack needs no restart.

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
