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
    local.luke.power.building.hotbar.Hotbars.cancelPending();
    FreeLook.reset((Minecraft) (Object) this);
    local.luke.power.autowalk.InventoryLook.stop();
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void power$screen(Screen screen, CallbackInfo ci) {
    if (screen != null) power$resetCamera();
    else local.luke.power.autowalk.InventoryLook.stop();
  }

  @Inject(method = "method_2115", at = @At("HEAD"))
  private void power$world(World world, String message, PlayerEntity player, CallbackInfo ci) {
    power$resetCamera();
  }
}
