package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.validation.TerrainDirty470Checks;
import net.minecraft.class_66;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_66.class, priority = 2400)
public class TerrainDirty470Capture {
  @Inject(method = "method_305", at = @At("HEAD"))
  private void mark(CallbackInfo ci) { TerrainDirty470Checks.mark((class_66)(Object)this); }
  @WrapMethod(method = "method_296")
  private void mesh(Operation<Void> original) {
    TerrainDirty470Checks.mesh((class_66)(Object)this); original.call();
  }
}
