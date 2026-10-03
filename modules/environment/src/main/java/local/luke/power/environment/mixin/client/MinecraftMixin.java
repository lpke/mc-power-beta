package local.luke.power.environment.mixin.client;

import local.luke.power.environment.Config;
import local.luke.power.music_api.MusicState;
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
    public void powerEnvironment_changeDimension(CallbackInfo ci) {
        if (Config.config.MUSIC_CONFIG.stopCurrentBgmOnPortalUse) {
            MusicState.cancelCurrentBGM = true;
        } else if (Config.config.MUSIC_CONFIG.stopDimensionSpecificSongOnPortalUse) {
            if (MusicState.songLevelId == this.player.dimensionId) {
                MusicState.cancelCurrentBGM = true;
            }
        }
    }
}
