package local.luke.power.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class MapConnectionMixin {
  @Inject(method = "<init>", at = @At("HEAD"))
  private static void power$mapServer(Minecraft mc, String address, int port, CallbackInfo ci) {
    local.luke.power.map.Maps.server(address, port);
  }
}
