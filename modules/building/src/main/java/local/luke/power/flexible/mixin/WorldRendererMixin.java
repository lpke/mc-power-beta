package local.luke.power.flexible.mixin;

import local.luke.power.flexible.PlacementOverlay;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class WorldRendererMixin {
  @Shadow private Minecraft field_2349;
  // After translucent terrain for each eye, before hand rendering replaces the world matrix.
  @Inject(method = "method_1841", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_555;method_1847(F)V"))
  private void overlay(float delta, long deadline, CallbackInfo ci) {
    if (field_2349.player != null && !field_2349.options.hideHud)
      PlacementOverlay.render(field_2349.player, field_2349.field_2823, delta);
  }
}
