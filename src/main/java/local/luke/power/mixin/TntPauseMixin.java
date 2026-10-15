package local.luke.power.mixin;

import local.luke.power.rules.*;
import local.luke.power.render.TntMotion;
import net.minecraft.class_365;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_365.class)
public class TntPauseMixin {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void power$pause(CallbackInfo ci) {
    class_365 tnt = (class_365)(Object)this;
    if (tnt.world.isRemote || WorldRules.enabled(tnt.world.method_262(), GameRule.TNT_EXPLODES)) return;
    tnt.prevX = tnt.x;
    tnt.prevY = tnt.y;
    tnt.prevZ = tnt.z;
    ((TntMotion.Access)tnt).power$tntMotion().clear();
    ci.cancel();
  }
}
