package local.luke.power.mixin;

import local.luke.power.render.CullingTerrain;
import net.minecraft.class_43;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.tr7zw.entityculling.CullTask", remap = false)
public abstract class CullingWorkerMixin {
  @Shadow @Final private Minecraft client;
  @Inject(method = "cullBlockEntities", at = @At("HEAD"), cancellable = true)
  private void power$knownProvider(CallbackInfo ci) {
    if (client.world == null || !CullingTerrain.supported(client.world)) ci.cancel();
  }
  @Redirect(method = "cullBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_214(II)Lnet/minecraft/class_43;", remap = true))
  private class_43 power$loadedOnly(World world, int x, int z) { return CullingTerrain.chunk(world, x, z); }

  @Redirect(method = "cullBlockEntities", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;method_1276(Lnet/minecraft/block/entity/BlockEntity;)Z", remap = true))
  private boolean power$rendererSnapshot(net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher dispatcher,
      net.minecraft.block.entity.BlockEntity entity) {
    return entity != null && ((local.luke.power.render.CullingRendererAccess)dispatcher).power$hasRenderer(entity.getClass());
  }
}
