package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.InventoryLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the event baseline while capturing: XQueryPointer can run ahead of queued X events. */
@Mixin(targets = "org.lwjgl.opengl.LinuxMouse", remap = false)
public abstract class InventoryLookMouseMixin {
  @Shadow private int last_x;
  @Shadow private int last_y;
  @Unique private int power$eventX, power$eventY;

  @Inject(method = "reset", at = @At("HEAD"))
  private void power$rememberEventPosition(boolean grab, boolean warp, CallbackInfo ci) {
    power$eventX = last_x; power$eventY = last_y;
  }

  @Inject(method = "reset", at = @At(value = "INVOKE",
      target = "Lorg/lwjgl/opengl/LinuxMouse;doHandlePointerMotion(ZZJIIIIJ)V"), cancellable = true)
  private void power$keepEventPosition(boolean grab, boolean warp, CallbackInfo ci) {
    if (!grab || !InventoryLook.capturing()) return;
    // The next queued motion must measure from the last dispatched position, not
    // the pointer's newer physical position. Normal motion still handles edge warps.
    last_x = power$eventX; last_y = power$eventY;
    ci.cancel();
  }
}
