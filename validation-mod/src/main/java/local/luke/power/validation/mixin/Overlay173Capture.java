package local.luke.power.validation.mixin;
import local.luke.power.validation.Release173Checks;
import net.minecraft.class_556;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(class_556.class)
public class Overlay173Capture {
  @Inject(method="method_1866",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Tessellator;startQuads()V"))
  private void water(CallbackInfo ci){if(Release173Checks.capture)Release173Checks.waterQuads++;}
  @Inject(method="method_1867",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Tessellator;startQuads()V"))
  private void fire(CallbackInfo ci){if(Release173Checks.capture)Release173Checks.fireQuads++;}
  @Inject(method="method_1861",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Tessellator;startQuads()V"))
  private void block(CallbackInfo ci){if(Release173Checks.capture)Release173Checks.blockQuads++;}
}
