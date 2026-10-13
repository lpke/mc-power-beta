package local.luke.power.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.class_555;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class ContainerPreviewMixin {
  @Shadow private Minecraft field_2349;
  @Inject(method = "method_1844", at = @At("TAIL"))
  private void power$preview(float delta, CallbackInfo ci) {
    local.luke.power.inventory.ContainerPreview.render(field_2349, delta);
  }
}
