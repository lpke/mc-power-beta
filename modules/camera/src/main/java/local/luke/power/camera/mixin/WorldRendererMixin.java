package local.luke.power.camera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.camera.Freecam;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow private Minecraft client;
    @Shadow private int entityRenderCooldown;

    // Keep the player body visible from the detached first-person camera.
    @Inject(at = @At("TAIL"), method = "renderEntities")
    private void freecam_cameraRenderer(Vec3d arg2, Culler f, float par3, CallbackInfo ci){
        if (entityRenderCooldown <= 0) {
            if(Freecam.freecamController.isActive() && !client.options.thirdPerson && !local.luke.power.input.DetachedCamera.hidesEntryBody()){
                EntityRenderDispatcher.INSTANCE.render(client.camera, par3);
            }
        }
    }
}
