package local.luke.power.debug;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Mouse.class,remap=false)
public abstract class DebugMouseMixin {
  @Inject(method="next",at=@At("RETURN"))
  private static void power$debugMouse(CallbackInfoReturnable<Boolean> cir) {
    if(cir.getReturnValue() && Mouse.getEventButton()>=0)
      DebugKeys.event(Mouse.getEventButton()-100,Mouse.getEventButtonState(),false);
  }
}
