package local.luke.power.inventory.tools.mixin;

import local.luke.power.inventory.tools.SavedCreativeHotbars;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Utility hotbar bindings share the same event decision as the native key loop and GUI. */
@Mixin(targets = "local.luke.power.controls.tweaks.morekeybinds.KeyPressedListener", remap = false)
public abstract class SavedCreativeControlKeysMixin {
  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
  private static void power$savedHotbar(CallbackInfo ci) {
    Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
    if (SavedCreativeHotbars.key(mc, Keyboard.getEventKey())) ci.cancel();
  }
}
