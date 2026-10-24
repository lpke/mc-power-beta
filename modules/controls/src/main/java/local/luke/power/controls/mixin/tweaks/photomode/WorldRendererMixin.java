package local.luke.power.controls.mixin.tweaks.photomode;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.controls.tweaks.photomode.PhotoModeScreen;
import local.luke.power.controls.tweaks.photomode.PhotoTerrainBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements PhotoTerrainBounds {
    @Shadow
    private Minecraft client;

    @Shadow private int minChunkX, minChunkZ, maxChunkX, maxChunkZ;

    @Override public double powerPhoto$centerX() { return ((double) minChunkX + maxChunkX + 16) / 2; }
    @Override public double powerPhoto$centerZ() { return ((double) minChunkZ + maxChunkZ + 16) / 2; }
    @Override public int powerPhoto$spanX() { return Math.max(16, maxChunkX - minChunkX + 16); }
    @Override public int powerPhoto$spanZ() { return Math.max(16, maxChunkZ - minChunkZ + 16); }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void powerPhoto$hideSky(float tickDelta, CallbackInfo ci) {
        // Beta's lower sky is a blue horizontal quad, not ocean terrain.
        if (this.client.currentScreen instanceof PhotoModeScreen) ci.cancel();
    }

    @ModifyExpressionValue(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;thirdPerson:Z"))
    public boolean renderPlayerInPhotoMode(boolean original) {
        if (this.client.currentScreen instanceof PhotoModeScreen photoScreen) {
            return photoScreen.renderPlayer;
        }
        
        return original;
    }

    @Inject(method = "renderClouds", at = @At(value = "HEAD"), cancellable = true)
    public void disableCloudsInPhotoMode(float par1, CallbackInfo ci) {
        if (this.client.currentScreen instanceof PhotoModeScreen) {
            ci.cancel();
        }
    }
}
