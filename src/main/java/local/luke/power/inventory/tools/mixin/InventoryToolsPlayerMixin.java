package local.luke.power.inventory.tools.mixin;

import local.luke.power.inventory.tools.InventoryToolsHost;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native PlayerScreen.init clears widgets without calling ContainerScreen.init. */
@Mixin(class_585.class)
public abstract class InventoryToolsPlayerMixin {
  @Inject(method = "init", at = @At("TAIL"))
  private void power$toolsPlayerInit(CallbackInfo ci) { ((InventoryToolsHost)this).power$installInventoryTools(); }
}
