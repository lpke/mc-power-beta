package local.luke.power.camera.mixin;

import net.glasslauncher.mods.gcapi3.api.GCAPI;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.modificationstation.stationapi.api.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.camera.Freecam;
import local.luke.power.camera.FreecamConfig;
import local.luke.power.camera.util.CameraPosition;


@Mixin(Entity.class)
public class EntityMixin {

    @Inject(at = @At("HEAD"), method = "changeLookDirection", cancellable = true)
    private void freecam_cameraMovementHandler(float yaw, float pitch, CallbackInfo ci){
        if(!((Entity) (Object)this instanceof ClientPlayerEntity) || !Freecam.freecamController.isActive()){
            return;
        }

        // Move freecam rotation
        if(!Freecam.freecamController.allowPlayerMovement && !Freecam.freecamController.updateSpeed){
            CameraPosition freecamPosition = Freecam.freecamController.getCameraPosition();

            float var3 = freecamPosition.pitch;
            float var4 = freecamPosition.yaw;
            freecamPosition.yaw = (float)((double)freecamPosition.yaw + (double)yaw * 0.15);
            freecamPosition.pitch = (float)((double)freecamPosition.pitch - (double)pitch * 0.15);
            if (freecamPosition.pitch < -90.0F) {
                freecamPosition.pitch = -90.0F;
            }

            if (freecamPosition.pitch > 90.0F) {
                freecamPosition.pitch = 90.0F;
            }
            if(freecamPosition.yaw > 360){
                freecamPosition.yaw -= 360;
            }
            if(freecamPosition.yaw < 0){
                freecamPosition.yaw = 360 + freecamPosition.yaw;
            }
            Freecam.freecamController.setCameraRotation(freecamPosition.pitch, freecamPosition.yaw, freecamPosition.roll);
        }

        // Update freecam speed
        if(Freecam.freecamController.updateSpeed){
            double percent = FreecamConfig.config.speed * 100 / local.luke.power.input.FlightMotion.HORIZONTAL_SPEED + yaw / 10;
            percent = Math.round(percent * 10) / 10d;
            FreecamConfig.config.speed = (float)Math.max(0, Math.min(1000,
                    percent * local.luke.power.input.FlightMotion.HORIZONTAL_SPEED / 100));
            GlassYamlFile modConfigFile = new GlassYamlFile();
            modConfigFile.set ("speed", FreecamConfig.config.speed);
            GCAPI.reloadConfig(Identifier.of("power_camera:config").toString(), modConfigFile);
        }

        if(!Freecam.freecamController.allowPlayerMovement){
            ci.cancel();
        }
    }
}
