package local.luke.power.validation.mixin;

import local.luke.power.validation.Validation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ValidationMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$validate(CallbackInfo ci) {
    Validation.tick((Minecraft) (Object) this);
  }
}
