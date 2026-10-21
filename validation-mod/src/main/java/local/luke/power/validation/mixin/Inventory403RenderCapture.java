package local.luke.power.validation.mixin;

import local.luke.power.validation.Inventory403Signals;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public class Inventory403RenderCapture {
  @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/container/ContainerScreen;drawBackground(F)V"))
  private void power$nativeBackground(int x,int y,float delta,CallbackInfo ci) {
    if(Inventory403Signals.count) Inventory403Signals.backgrounds++;
  }
}
