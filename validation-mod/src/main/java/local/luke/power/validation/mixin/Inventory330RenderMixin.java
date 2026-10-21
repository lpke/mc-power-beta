package local.luke.power.validation.mixin;

import local.luke.power.validation.Inventory330Capture;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public abstract class Inventory330RenderMixin {
  @Inject(method="render",at=@At("HEAD"))
  private void start(int x,int y,float delta,CallbackInfo ci){Inventory330Capture.start();}
  @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/container/ContainerScreen;drawBackground(F)V"))
  private void gui(int x,int y,float delta,CallbackInfo ci){Inventory330Capture.gui();}
}
