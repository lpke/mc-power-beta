package local.luke.power.worldedit.mixin;

import local.luke.power.worldedit.SelectionRenderer;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class WorldRendererMixin {
  @Shadow private Minecraft field_2349;

  @Inject(
      method = "method_1841",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_555;method_1847(F)V"))
  private void worldedit$render(float delta, long end, CallbackInfo ci) {
    SelectionRenderer.render(field_2349, delta);
  }
}
