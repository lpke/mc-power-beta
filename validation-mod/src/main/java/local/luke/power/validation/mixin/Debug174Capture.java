package local.luke.power.validation.mixin;

import local.luke.power.validation.Release174Checks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class Debug174Capture {
  @Shadow private Minecraft minecraft;

  @Inject(method="render",at=@At("HEAD"))
  private void power$debugFrame(float delta,boolean focused,int mx,int my,CallbackInfo ci) {
    Release174Checks.frame(minecraft);
  }
}
