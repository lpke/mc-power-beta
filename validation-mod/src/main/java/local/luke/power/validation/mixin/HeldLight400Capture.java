package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.HeldLight400Checks;
import local.luke.power.validation.OffhandActions400Checks;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = class_555.class, priority = 2000)
public class HeldLight400Capture {
  @Shadow private Minecraft field_2349;
  @WrapMethod(method = "method_1841")
  private void power$frame(float delta, long deadline, Operation<Void> original) {
    HeldLight400Checks.beforeFrame(field_2349);
    try { original.call(delta, deadline); }
    finally {
      try { HeldLight400Checks.afterFrame(field_2349, delta); }
      finally { OffhandActions400Checks.afterFrame(field_2349, delta); }
    }
  }
}
