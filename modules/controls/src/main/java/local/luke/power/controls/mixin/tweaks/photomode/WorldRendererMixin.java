package local.luke.power.controls.mixin.tweaks.photomode;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.controls.tweaks.photomode.PhotoModeScreen;
import local.luke.power.controls.tweaks.photomode.PhotoTerrainBounds;
import local.luke.power.input.PhotoProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
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
    @Shadow private ChunkBuilder[] chunks;

    @Override public double powerPhoto$centerX() { return ((double) minChunkX + maxChunkX + 16) / 2; }
    @Override public double powerPhoto$centerZ() { return ((double) minChunkZ + maxChunkZ + 16) / 2; }
    @Override public int powerPhoto$spanX() { return Math.max(16, maxChunkX - minChunkX + 16); }
    @Override public int powerPhoto$spanZ() { return Math.max(16, maxChunkZ - minChunkZ + 16); }

    @Override public PhotoProjection.InitialView powerPhoto$initialView(
            int physicalWidth, int physicalHeight, double tiltDegrees, double yawDegrees) {
        var frame = new PhotoProjection.TerrainFrame(tiltDegrees, yawDegrees);
        double centerX = powerPhoto$centerX(), centerZ = powerPhoto$centerZ();
        var camera = this.client.camera;
        double eyeY = camera.y - (camera.standingEyeHeight - 1.62F);
        boolean included = false;
        // One bounded pass on screen entry. Empty built meshes need no room; unfinished
        // meshes retain their complete section bounds so later compilation cannot enlarge the fit.
        if (this.chunks != null && this.chunks.length <= 65536) {
            for (ChunkBuilder section : this.chunks) {
                if (section == null) { included = false; break; }
                if (section.hasNoGeometry()) continue;
                frame.include(section.x - centerX, section.y - eyeY, section.z - centerZ - 0.1,
                    section.x + section.sizeX - centerX, section.y + section.sizeY - eyeY,
                    section.z + section.sizeZ - centerZ - 0.1);
                included = true;
            }
        }
        if (!included) {
            frame.include(-powerPhoto$spanX() / 2d, -eyeY, -powerPhoto$spanZ() / 2d - 0.1,
                powerPhoto$spanX() / 2d, 128 - eyeY, powerPhoto$spanZ() / 2d - 0.1);
        }
        return frame.initialView(physicalWidth, physicalHeight);
    }

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
