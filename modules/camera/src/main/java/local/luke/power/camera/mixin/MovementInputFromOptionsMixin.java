package local.luke.power.camera.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.camera.Freecam;

@Mixin(KeyboardInput.class)
public class MovementInputFromOptionsMixin extends Input {
    @Shadow private boolean[] keys;

    // Reroute player movement to freecam
    @Inject(at = @At("HEAD"), method = "update", cancellable = true)
    private void freecam_cameraMovementHandler(PlayerEntity par1, CallbackInfo ci) {
        if(!Freecam.freecamController.isActive()){
            return;
        }
        Freecam.freecamController.clearMovementInput();
        if(!Freecam.freecamController.allowPlayerMovement){
            net.minecraft.client.Minecraft mc = (net.minecraft.client.Minecraft)
                net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
            boolean input = mc.currentScreen == null && org.lwjgl.opengl.Display.isActive();
            float move = 0f;
            float strafe = 0f;
            movementSideways = 0f;
            // Auto-walk drives the player independently of the camera's manual keys.
            movementForward = local.luke.power.input.MovementOwnership.autoWalking() ? 1f : 0f;
            jumping = false;
            sneaking = false;

            if(input && this.keys[0]) {
                ++move;
            }

            if(input && this.keys[1]) {
                --move;
            }

            if(input && this.keys[2]) {
                ++strafe;
            }

            if(input && this.keys[3]) {
                --strafe;
            }

            Freecam.freecamController.move = move;
            Freecam.freecamController.strafe = strafe;
            Freecam.freecamController.jumping = input && keys[4];
            Freecam.freecamController.sneaking = input && keys[5];
        }
        if(!Freecam.freecamController.allowPlayerMovement){
            ci.cancel();
        }
    }
}
