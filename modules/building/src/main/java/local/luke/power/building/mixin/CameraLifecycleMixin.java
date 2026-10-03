package local.luke.power.building.mixin;

import local.luke.power.building.camera.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class CameraLifecycleMixin {
  private void power$resetCamera() {
    FreeLook.reset((Minecraft) (Object) this);
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void power$screen(Screen screen, CallbackInfo ci) {
    if (screen != null) power$resetCamera();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void power$world(World world, String message, PlayerEntity player, CallbackInfo ci) {
    power$resetCamera();
  }
}
