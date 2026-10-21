package local.luke.power.mixin;

import local.luke.power.map.RadarIcons;
import net.minecraft.class_555;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public final class RadarFrameMixin {
  @Inject(method = "method_1844", at = @At("HEAD"))
  private void power$radarFrame(float delta, CallbackInfo ci) { RadarIcons.beginFrame(); local.luke.power.map.Minimap.frameDelta(delta); }
}
