package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.InventoryLook;
import net.minecraft.class_585;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The model follows the held cursor origin; slot hover still receives hidden coordinates. */
@Mixin(class_585.class)
public abstract class InventoryLookModelMixin extends Screen {
  @Shadow private float field_2523;
  @Shadow private float field_2524;

  @Inject(method = "render", at = @At("TAIL"))
  private void power$modelOrigin(int x, int y, float delta, CallbackInfo ci) {
    if (InventoryLook.active()) {
      field_2523 = InventoryLook.cursorX(this, minecraft);
      field_2524 = InventoryLook.cursorY(this, minecraft);
    }
  }
}
