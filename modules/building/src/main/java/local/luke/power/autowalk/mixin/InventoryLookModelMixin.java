package local.luke.power.autowalk.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.autowalk.InventoryLook;
import net.minecraft.class_585;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Native and Creative hover caches never supply the player model's cursor. */
@Mixin(class_585.class)
public abstract class InventoryLookModelMixin extends Screen {
  @Unique private float power$modelCursorX;
  @Unique private float power$modelCursorY;

  @WrapMethod(method = "render")
  private void power$modelOrigin(int x, int y, float delta, Operation<Void> original) {
    // Creative writes the hover cache at HEAD and can cancel native rendering.
    // Set the independent model cursor before either path can draw or cancel.
    power$modelCursorX = InventoryLook.active() ? InventoryLook.cursorX(this, minecraft) : x;
    power$modelCursorY = InventoryLook.active() ? InventoryLook.cursorY(this, minecraft) : y;
    original.call(x, y, delta);
  }

  @ModifyExpressionValue(method = "drawBackground", at = @At(value = "FIELD",
      target = "Lnet/minecraft/class_585;field_2523:F"))
  private float power$modelX(float hoverX) {
    return power$modelCursorX;
  }

  @ModifyExpressionValue(method = "drawBackground", at = @At(value = "FIELD",
      target = "Lnet/minecraft/class_585;field_2524:F"))
  private float power$modelY(float hoverY) {
    return power$modelCursorY;
  }
}
