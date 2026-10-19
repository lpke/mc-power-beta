package local.luke.power.validation.mixin;

import local.luke.power.map.MapMath;
import local.luke.power.ui.WorldMapScreen;
import local.luke.power.validation.MapPanFrames;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldMapScreen.class, remap = false)
public abstract class MapPan300Capture {
  @Shadow @Final private MapMath.View view;
  @Shadow private boolean dragging;

  // Production Screen.render is method_118 after Loom remaps this custom subclass.
  @Inject(method = "method_118(IIF)V", at = @At("TAIL"), remap = false)
  private void power$capturePan(int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (MapPanFrames.recording()) MapPanFrames.frame(this, view.x, view.z, dragging);
  }
}
