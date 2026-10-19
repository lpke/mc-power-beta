package local.luke.power.mixin;

import local.luke.power.map.Maps;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MapTerrainUpdatesMixin {
  @Inject(method = "method_1149", at = @At("RETURN"))
  private void power$mapBlock(int x, int y, int z, CallbackInfo ci) {
    if (Maps.sampler() != null) Maps.sampler().invalidate(x, z, x, z);
  }

  @Inject(method = "method_1150", at = @At("RETURN"))
  private void power$mapArea(int x, int y, int z, int maxX, int maxY, int maxZ, CallbackInfo ci) {
    if (Maps.sampler() != null) Maps.sampler().invalidate(x, z, maxX, maxZ);
  }
}
