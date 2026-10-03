package local.luke.power.worldedit.mixin;

import local.luke.power.worldedit.carry.*;
import net.minecraft.class_556;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_556.class)
public abstract class CarryFirstPersonMixin {
  @Shadow private Minecraft field_2401;

  @Inject(method = "method_1860", at = @At("HEAD"), cancellable = true)
  private void power$carry(float delta, CallbackInfo ci) {
    if (ContainerCarry.visible(field_2401)) {
      CarryRenderer.firstPerson(field_2401, delta);
      ci.cancel();
    }
  }
}
