package local.luke.power.mixin;

import local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener;
import local.luke.power.input.Bindings;
import local.luke.power.ui.WorldMapScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The normal remappable screenshot key captures map terrain once per press. */
@Mixin(Minecraft.class)
public abstract class MapScreenshotKeyMixin {
  @Unique private boolean power$mapScreenshotHeld;
  @Inject(method="method_2152()V",at=@At("HEAD"),cancellable=true)
  private void power$mapScreenshot(CallbackInfo ci) {
    Minecraft mc=(Minecraft)(Object)this;
    if(!(mc.currentScreen instanceof WorldMapScreen map)) {
      if(power$mapScreenshotHeld) {
        power$mapScreenshotHeld=Bindings.down(KeyBindingListener.takeScreenshot);
        if(power$mapScreenshotHeld)ci.cancel();
      }
      return;
    }
    ci.cancel();
    boolean down=Display.isActive()&&Bindings.down(KeyBindingListener.takeScreenshot);
    if(down&&!power$mapScreenshotHeld&&!map.capturingBinding())map.requestScreenshot();
    power$mapScreenshotHeld=down;
  }
}
