package local.luke.power.mixin;

import local.luke.power.render.WaterTerrain;
import net.minecraft.class_42;
import net.minecraft.class_43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Cache only native render-region IDs inside the owning synchronous mesh build. */
@Mixin(class_42.class)
public class TerrainBlockCacheMixin {
  @Redirect(method = "getBlockId", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_43;method_859(III)I"))
  private int power$meshBlock(class_43 chunk, int localX, int blockY, int localZ, int x, int y, int z) {
    return WaterTerrain.block(((RenderBlockViewAccessor)this).power$world(), chunk, x, y, z);
  }
}
