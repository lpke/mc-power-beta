package local.luke.tweaks.mixin;

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
  private void lpke$keyLabel(String key, CallbackInfoReturnable<String> ci) {
    String name =
        switch (key) {
          case "key.omnilook.toggle" -> "Free Look";
          case "lpketweaks.hotbar.base" -> "Hotbar Preview / Row Modifier";
          case "lpketweaks.hotbar.scroll" -> "Hotbar Scroll, Hold";
          case "lpketweaks.hotbar.row1" -> "Swap Hotbar with Top Row";
          case "lpketweaks.hotbar.row2" -> "Swap Hotbar with Middle Row";
          case "lpketweaks.hotbar.row3" -> "Swap Hotbar with Bottom Row";
          default -> null;
        };
    if (name != null) ci.setReturnValue(name);
  }
}
