package local.luke.power.building.mixin;

import local.luke.power.building.hotbar.Hotbars;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ControlFeatures handles number keys separately from Minecraft's keyboard loop. */
@Pseudo
@Mixin(
    targets = "local.luke.power.controls.tweaks.morekeybinds.KeyPressedListener",
    remap = false)
public abstract class HotbarControlFeaturesMixin {
  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true, require = 0)
  private static void power$consumeRowKey(CallbackInfo ci) {
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    if (Hotbars.consumesKey(mc, Keyboard.getEventKey())) ci.cancel();
  }
}
