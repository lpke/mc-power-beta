package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.AutoWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void updateToggle(CallbackInfo ci) {
    AutoWalk.tick((Minecraft) (Object) this);
  }

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void stopForScreen(Screen screen, CallbackInfo ci) {
    if (screen != null) AutoWalk.stop();
  }
}
