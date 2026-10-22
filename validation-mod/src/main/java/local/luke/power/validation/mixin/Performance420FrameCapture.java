package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.Performance420Checks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;

@Mixin(value = class_555.class, priority = 2100)
public class Performance420FrameCapture {
  @Shadow private Minecraft field_2349;
  @WrapMethod(method = "method_1841")
  private void power$frame(float delta, long deadline, Operation<Void> original) {
    if (Performance420Checks.ui()) { original.call(delta, deadline); return; }
    Performance420Checks.beforeFrame(field_2349);
    try { original.call(delta, deadline); }
    finally { Performance420Checks.afterFrame(field_2349); }
  }
  @WrapMethod(method = "method_1844")
  private void power$ui(float delta, Operation<Void> original) {
    boolean measure = Performance420Checks.ui();
    if (measure) Performance420Checks.beforeFrame(field_2349);
    try { original.call(delta); }
    finally { if (measure) Performance420Checks.afterFrame(field_2349); }
  }
}
