package local.luke.power.mixin;

import net.minecraft.class_345;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Truncating distance differences makes near-equal chunks non-transitive under TimSort. */
@Mixin(class_345.class)
public abstract class ChunkDistanceOrderMixin {
  @Shadow private double field_1309, field_1310, field_1311;

  @Inject(method = "compare(Lnet/minecraft/class_66;Lnet/minecraft/class_66;)I", at = @At("HEAD"), cancellable = true)
  private void power$order(class_66 first, class_66 second, CallbackInfoReturnable<Integer> cir) {
    double ax = first.field_245 + field_1309, ay = first.field_246 + field_1310, az = first.field_247 + field_1311;
    double bx = second.field_245 + field_1309, by = second.field_246 + field_1310, bz = second.field_247 + field_1311;
    cir.setReturnValue(Double.compare(ax * ax + ay * ay + az * az, bx * bx + by * by + bz * bz));
  }
}
