package local.luke.power.validation.mixin;

import local.luke.power.validation.Validation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ValidationMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void power$beginTick(CallbackInfo ci) { local.luke.power.validation.HeldLight400Checks.beforeTick((Minecraft)(Object)this); }
  @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "run", remap = false, at = @At(value = "INVOKE", target = "Ljava/lang/Throwable;printStackTrace()V"))
  private void power$crashReport(Throwable error, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
    try (var writer = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(java.nio.file.Path.of("power-beta-validation-crash.log")))) {
      error.printStackTrace(writer);
    } catch (java.io.IOException ignored) {}
    original.call(error);
  }
  @Inject(method = "tick", at = @At("TAIL"))
  private void power$validate(CallbackInfo ci) {
    local.luke.power.validation.HeldLight400Checks.afterTick((Minecraft)(Object)this);
    Validation.tick((Minecraft) (Object) this);
  }
}
