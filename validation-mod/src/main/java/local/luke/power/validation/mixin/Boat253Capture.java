package local.luke.power.validation.mixin;

import local.luke.power.validation.Release253Checks;
import net.minecraft.class_113;
import net.minecraft.class_172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_172.class)
public class Boat253Capture {
  @Inject(method="render(Lnet/minecraft/class_113;DDDFF)V",at=@At("RETURN"))
  private void power$boat(class_113 boat,double x,double y,double z,float yaw,float delta,CallbackInfo ci){
    if(Release253Checks.captureBoats)Release253Checks.renderedBoats.add(boat);
  }
}
