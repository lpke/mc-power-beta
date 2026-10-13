package local.luke.power.validation.mixin;

import local.luke.power.validation.Validation;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ContainerScreen.class, class_585.class})
public abstract class InventoryLookRenderMixin {
  @Inject(method = "render", at = @At("HEAD"))
  private void power$pointer(int x, int y, float delta, CallbackInfo ci) {
    Validation.inventoryMouseX = x; Validation.inventoryMouseY = y;
  }
}
