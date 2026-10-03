package local.luke.power.building.mixin;

import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TranslationStorage.class)
public abstract class KeyTranslationsMixin {
  @Inject(
      method = "get(Ljava/lang/String;)Ljava/lang/String;",
      at = @At("HEAD"),
      cancellable = true)
  private void power$keyLabel(String key, CallbackInfoReturnable<String> ci) {
    String name =
        switch (key) {
          case "key.powerbeta.free_look" -> "Free Look";
          case "power_building.hotbar.base" -> "Hotbar Preview / Row Modifier";
          case "power_building.hotbar.scroll" -> "Hotbar Scroll, Hold";
          case "power_building.hotbar.row1" -> "Swap Hotbar with Top Row";
          case "power_building.hotbar.row2" -> "Swap Hotbar with Middle Row";
          case "power_building.hotbar.row3" -> "Swap Hotbar with Bottom Row";
          default -> null;
        };
    if (name != null) ci.setReturnValue(name);
  }
}
