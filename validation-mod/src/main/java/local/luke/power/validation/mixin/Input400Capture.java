package local.luke.power.validation.mixin;

import local.luke.power.inventory.tools.SavedCreativeHotbars;
import local.luke.power.validation.Input400Checks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observers never invoke an action or replace the native event result. */
@Mixin(value = SavedCreativeHotbars.class, remap = false)
public abstract class Input400Capture {
  @Inject(method = "key", at = @At("HEAD"))
  private static void power$numberRead(Minecraft mc, int key, CallbackInfoReturnable<Boolean> ci) { Input400Checks.observeKey(key); }
  @Inject(method = "save", at = @At("HEAD"))
  private static void power$save(Minecraft mc, int row, CallbackInfoReturnable<Boolean> ci) { Input400Checks.observeAction(false, row); }
  @Inject(method = "load", at = @At("HEAD"))
  private static void power$load(Minecraft mc, int row, CallbackInfoReturnable<Boolean> ci) { Input400Checks.observeAction(true, row); }
}
