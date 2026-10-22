package local.luke.power.mixin;

import java.util.Map;
import local.luke.power.render.CullingRendererAccess;
import local.luke.power.render.CullingRenderers;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BlockEntityRenderDispatcher.class)
public class CullingRendererMixin implements CullingRendererAccess {
  @Shadow private Map<Class<?>, BlockEntityRenderer> field_1564;
  @Unique private final CullingRenderers power$renderers = new CullingRenderers();

  @Inject(method = "method_1273", at = @At("HEAD"))
  private void power$publish(World world, CallbackInfo ci) {
    power$renderers.publish(field_1564);
  }

  @Inject(method = "method_1275", at = @At("RETURN"))
  private void power$resolved(Class<?> type, CallbackInfoReturnable<BlockEntityRenderer> cir) {
    power$renderers.resolved(type, cir.getReturnValue() != null);
  }

  @Override public boolean power$hasRenderer(Class<?> type) { return power$renderers.hasRenderer(type); }
}
