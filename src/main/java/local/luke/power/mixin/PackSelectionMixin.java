package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.visual.PackSelection;
import net.minecraft.class_303;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_303.class)
public class PackSelectionMixin {
  @WrapOperation(method = "method_999", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;save()V"))
  private void power$preview(GameOptions options, Operation<Void> original) {
    if (!PackSelection.previewing()) original.call(options);
  }
}
