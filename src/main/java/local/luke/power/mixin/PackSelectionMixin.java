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
    var client = (net.minecraft.client.Minecraft) net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if (!PackSelection.previewing()
        && !(client.currentScreen instanceof local.luke.power.ui.TexturePackScreen)) original.call(options);
  }
}
