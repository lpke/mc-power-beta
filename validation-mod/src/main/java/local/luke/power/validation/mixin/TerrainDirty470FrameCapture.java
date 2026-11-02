package local.luke.power.validation.mixin;

import local.luke.power.validation.TerrainDirty470Checks;
import net.minecraft.class_555;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_555.class)
public class TerrainDirty470FrameCapture {
  @Inject(method = "method_1841", at = @At("HEAD"))
  private void frame(float delta, long deadline, CallbackInfo ci) { TerrainDirty470Checks.frame(); }
}
