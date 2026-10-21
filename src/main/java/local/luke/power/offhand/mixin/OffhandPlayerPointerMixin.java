package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.offhand.OffhandSlotPicker;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;

/** The Creative catalog replaces the player screen render before its superclass runs. */
@Mixin(class_585.class)
public abstract class OffhandPlayerPointerMixin {
  @WrapMethod(method = "render")
  private void power$offhandPointer(int x, int y, float delta, Operation<Void> original) {
    ((OffhandSlotPicker)(Object)this).power$offhandPointer(x, y);
    original.call(x, y, delta);
  }
}
