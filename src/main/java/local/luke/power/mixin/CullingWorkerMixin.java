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
}
