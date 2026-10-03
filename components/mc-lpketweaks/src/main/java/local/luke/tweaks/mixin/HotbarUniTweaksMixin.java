package local.luke.tweaks.mixin;

import local.luke.tweaks.hotbar.Hotbars;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** UniTweaks handles number keys separately from Minecraft's keyboard loop. */
@Pseudo
@Mixin(
    targets = "net.danygames2014.unitweaks.tweaks.morekeybinds.KeyPressedListener",
    remap = false)
public abstract class HotbarUniTweaksMixin {
  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
  private static void lpke$consumeRowKey(CallbackInfo ci) {
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    if (Hotbars.consumesKey(mc, Keyboard.getEventKey())) ci.cancel();
  }
}
