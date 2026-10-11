package local.luke.power.mixin;

import local.luke.power.render.CullingTerrain;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "dev.tr7zw.entityculling.Provider", remap = false)
public abstract class CullingProviderMixin {
  @Redirect(method = "isOpaqueFullCube", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I", remap = true))
  private int power$loadedBlock(World world, int x, int y, int z) {
    return CullingTerrain.block(world, x, y, z);
  }
}
