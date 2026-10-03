package local.luke.tweaks.mixin;

import local.luke.tweaks.camera.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class CameraLifecycleMixin {
  private void lpke$resetCamera() {
    FreeLook.reset((Minecraft) (Object) this);
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void lpke$screen(Screen screen, CallbackInfo ci) {
    if (screen != null) lpke$resetCamera();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void lpke$world(World world, String message, PlayerEntity player, CallbackInfo ci) {
    lpke$resetCamera();
  }
}
