package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.*;

/** Wrap the complete mesh, including Power Beta's optional color endpoints. */
@Mixin(value = class_66.class, priority = 2200)
public class Performance420TerrainCapture {
  @Shadow public boolean field_249;
  @WrapMethod(method = "method_296")
  private void power$mesh(Operation<Void> original) {
    long start = field_249 ? Performance420Checks.start() : 0;
    try { original.call(); }
    finally { Performance420Checks.measured("terrainBuild", start); }
  }
}
