package local.luke.power.camera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.camera.Freecam;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void power$speedKey(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        Freecam.freecamController.updateSpeed = mc.currentScreen == null && org.lwjgl.opengl.Display.isActive()
            && local.luke.power.input.Bindings.down(local.luke.power.camera.registry.KeyBindingRegistry.changeSpeedKeybinding);
    }

    @Inject(at = @At("HEAD"), method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V")
    private void power$resetCamera(World world, String message, PlayerEntity player, CallbackInfo ci) {
        Freecam.freecamController.setActive(false);
        Freecam.freecamController.cameraPositionSet = false;
    }

    // Load camerapositions from json when entering world
    @Inject(at = @At("TAIL"), method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V")
    private void freecam_savedCameraPositionLoader(World string, String arg2, PlayerEntity par3, CallbackInfo ci){
        Freecam.freecamController.loadSavedCameraPositions(string);
    }
}
