package local.luke.power.inventory.tools.mixin;

import local.luke.power.inventory.tools.InventoryToolsHost;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress the player model and Creative tabs along with the native inventory body. */
@Mixin(value = class_585.class, priority = 3000)
public abstract class InventoryToolsPlayerRenderMixin {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void power$onlyLayer(int x, int y, float delta, CallbackInfo ci) {
    if (((InventoryToolsHost)this).power$drawInventoryTools(x, y, delta)) ci.cancel();
  }
}
