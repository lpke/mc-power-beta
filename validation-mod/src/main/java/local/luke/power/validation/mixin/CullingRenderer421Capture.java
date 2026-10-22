package local.luke.power.validation.mixin;

import local.luke.power.validation.Culling421Checks;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class CullingRenderer421Capture {
  @Inject(method = "method_1275", at = @At("HEAD"))
  private void power$thread(Class<?> type, CallbackInfoReturnable<BlockEntityRenderer> cir) {
    if (Thread.currentThread().getName().equals("CullThread")) Culling421Checks.backgroundRendererReads.incrementAndGet();
  }
}
