package local.luke.power.mixin;

import local.luke.power.light.LightOverlay;
import local.luke.power.map.WaypointMarkers;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public abstract class LightOverlayMixin {
  @Shadow private Minecraft field_2349;

  @Inject(method = "method_1841", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_555;method_1840(FI)V"), cancellable = true)
  private void power$mapBackground(float delta, long end, CallbackInfo ci) {
    // Frame-owned camera maintenance has already run. The opaque map replaces the entire scene.
    if (field_2349.currentScreen instanceof local.luke.power.ui.WorldMapScreen) {
      org.lwjgl.opengl.GL11.glViewport(0, 0, field_2349.displayWidth, field_2349.displayHeight);
      org.lwjgl.opengl.GL11.glColorMask(true, true, true, true);
      ci.cancel();
    }
  }

  @Inject(method = "method_1840", at = @At("HEAD"))
  private void power$beginWaypointCamera(float delta, int eye, CallbackInfo ci) { WaypointMarkers.beginCamera(field_2349); }

  @WrapOperation(method = "method_1840", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_555;method_1850(F)V"))
  private void power$waypointBob(class_555 renderer, float delta, Operation<Void> original) {
    WaypointMarkers.beforeBob();
    original.call(renderer, delta);
    WaypointMarkers.afterBob();
  }

  @Inject(method = "method_1841", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_555;method_1847(F)V"))
  private void power$light(float delta, long end, CallbackInfo ci) {
    LightOverlay.render(field_2349, delta);
    local.luke.power.debug.DebugWorld.render(field_2349, delta);
  }

  @Inject(method = "method_1841", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/class_555;method_1840(FI)V", shift = At.Shift.AFTER))
  private void power$waypointCamera(float delta, long end, CallbackInfo ci) {
    local.luke.power.map.WaypointMarkers.project(field_2349, delta);
  }
}
