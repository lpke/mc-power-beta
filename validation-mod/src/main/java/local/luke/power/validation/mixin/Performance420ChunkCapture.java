package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_326.class)
public class Performance420ChunkCapture {
  @WrapMethod(method = "method_1051")
  private class_43 power$disk(int x, int z, Operation<class_43> original) {
    long start = Performance420Checks.start();
    try { return original.call(x, z); }
    finally { Performance420Checks.measured("chunkDiskRead", start); }
  }
  @WrapOperation(method = "method_1807", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_51;method_1806(II)Lnet/minecraft/class_43;"))
  private class_43 power$generate(class_51 generator, int x, int z, Operation<class_43> original) {
    long start = Performance420Checks.start();
    try { return original.call(generator, x, z); }
    finally { Performance420Checks.measured("chunkGenerate", start); }
  }
  @WrapMethod(method = "method_1803")
  private void power$populate(class_51 provider, int x, int z, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(provider, x, z); }
    finally { Performance420Checks.measured("chunkPopulate", start); }
  }
}
