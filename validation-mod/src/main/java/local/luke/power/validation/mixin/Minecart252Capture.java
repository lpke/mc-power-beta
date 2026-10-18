package local.luke.power.validation.mixin;

import local.luke.power.validation.Release252Checks;
import net.minecraft.class_549;
import net.minecraft.class_576;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_576.class)
public class Minecart252Capture {
  @Inject(method="render(Lnet/minecraft/class_549;DDDFF)V",at=@At("RETURN"))
  private void power$cart(class_549 cart,double x,double y,double z,float yaw,float delta,CallbackInfo ci){
    if(Release252Checks.captureCarts)Release252Checks.renderedCarts.add(cart);
  }
}
