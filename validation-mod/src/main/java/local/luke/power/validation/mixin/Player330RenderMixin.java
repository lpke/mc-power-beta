package local.luke.power.validation.mixin;

import local.luke.power.validation.Inventory330Capture;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=class_585.class,priority=1200)
public abstract class Player330RenderMixin {
  @Inject(method="render",at=@At("HEAD"))
  private void start(int x,int y,float delta,CallbackInfo ci){Inventory330Capture.start();}
  @Inject(method="drawBackground",at=@At("HEAD"))
  private void gui(float delta,CallbackInfo ci){Inventory330Capture.gui();}
}
