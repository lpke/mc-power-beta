package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.world.World;
import net.minecraft.class_62;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class Performance421WorldCapture {
  @WrapMethod(method = "method_195")
  private void power$save(boolean all, class_62 progress, Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(all, progress); }
    finally { Performance420Checks.measured("worldSave", start); }
  }
  @WrapMethod(method = "method_248")
  private void power$randomTicks(Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(); }
    finally { Performance420Checks.measured("randomTicks", start); }
  }
  @WrapMethod(method = "method_242")
  private void power$worldTick(Operation<Void> original) {
    long start = Performance420Checks.start();
    try { original.call(); }
    finally { Performance420Checks.measured("worldTick", start); }
  }
}
