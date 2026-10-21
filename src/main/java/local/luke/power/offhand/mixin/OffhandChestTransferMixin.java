package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.offhand.OffhandNativeSlot;
import net.minecraft.class_506;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native chest transfers end at the hotbar, before the player-owned side slot. */
@Mixin(class_506.class)
public abstract class OffhandChestTransferMixin extends Container {
  @ModifyExpressionValue(method = "method_2086", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
  private int power$nativePlayerEnd(int size) {
    return size > 0 && slots.get(size - 1) instanceof OffhandNativeSlot ? size - 1 : size;
  }
}
