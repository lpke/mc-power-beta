package local.luke.power.autowalk.mixin;

import local.luke.power.autowalk.InventoryLook;
import local.luke.power.autowalk.CursorRelease;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep cursor transitions in X event order, independently of XQueryPointer's newer position. */
@Mixin(targets = "org.lwjgl.opengl.LinuxMouse", remap = false)
public abstract class InventoryLookMouseMixin {
  @Shadow private int last_x;
  @Shadow private int last_y;
  @Shadow private void doWarpPointer(int x, int y) { throw new AssertionError(); }
  @Shadow private int transformY(int y) { throw new AssertionError(); }
  @Unique private int power$eventX, power$eventY;
  @Unique private CursorRelease power$release;

  @Unique private CursorRelease power$cursorRelease() {
    if (power$release == null) power$release = new CursorRelease();
    return power$release;
  }

  @Inject(method = "setCursorPosition", at = @At("HEAD"), cancellable = true)
  private void power$orderedRelease(int x, int y, CallbackInfo ci) {
    if (!power$cursorRelease().restore(x, y)) return;
    // This is the saved-position warp inside Mouse.setGrabbed(false), while the
    // native pointer is still hidden. Use LWJGL's own ordered warp marker so old
    // gameplay MotionNotify events cannot move the newly visible GUI pointer.
    doWarpPointer(x, transformY(y));
    ci.cancel();
  }

  @Inject(method = "doWarpPointer", at = @At("HEAD"))
  private void power$warpIssued(int x, int y, CallbackInfo ci) {
    power$cursorRelease().warpIssued();
  }

  @Inject(method = "handleWarpEvent", at = @At("TAIL"))
  private void power$warpHandled(int x, int y, CallbackInfo ci) {
    power$cursorRelease().warpHandled();
  }

  @Inject(method = "handlePointerMotion", at = @At("HEAD"), cancellable = true)
  private void power$discardOldMotion(boolean grab, boolean warp, long millis, long root,
      int rootX, int rootY, int x, int y, CallbackInfo ci) {
    if (power$cursorRelease().pending()) ci.cancel();
  }

  @Inject(method = "reset", at = @At("HEAD"))
  private void power$rememberEventPosition(boolean grab, boolean warp, CallbackInfo ci) {
    power$eventX = last_x; power$eventY = last_y;
    power$cursorRelease().grabbed(grab);
  }

  @Inject(method = "reset", at = @At(value = "INVOKE",
      target = "Lorg/lwjgl/opengl/LinuxMouse;doHandlePointerMotion(ZZJIIIIJ)V"), cancellable = true)
  private void power$keepEventPosition(boolean grab, boolean warp, CallbackInfo ci) {
    if (!grab && power$cursorRelease().pending()) {
      // XQueryPointer can still report the pre-warp position. Mouse.poll runs
      // inside setGrabbed, so publish the requested position before returning.
      last_x = power$cursorRelease().x(); last_y = power$cursorRelease().y();
      ci.cancel();
      return;
    }
    if (!grab || !InventoryLook.capturing()) return;
    // The next queued motion must measure from the last dispatched position, not
    // the pointer's newer physical position. Normal motion still handles edge warps.
    last_x = power$eventX; last_y = power$eventY;
    ci.cancel();
  }
}
