package local.luke.building.validation.mixin;

import local.luke.building.validation.ValidationRun;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ValidationMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void time(CallbackInfo ci) {
    ValidationRun.beginTick();
  }

  @Inject(method = "tick", at = @At("TAIL"))
  private void test(CallbackInfo ci) {
    ValidationRun.tick((Minecraft) (Object) this);
  }
}
