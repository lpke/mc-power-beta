package local.luke.power.mixin;

import local.luke.power.render.CullingTerrain;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "dev.tr7zw.entityculling.EntityCullingMod", remap = false)
public abstract class CullingBlockEntityMixin {
  @Redirect(method = "setupBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;getBlock()Lnet/minecraft/block/Block;", remap = true))
  private Block power$loadedBlock(BlockEntity entity) {
    int id = entity.world == null ? 0 : CullingTerrain.block(entity.world, entity.x, entity.y, entity.z);
    // A removed or unloaded block entity gets a conservative full-block box for this pass.
    return id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null ? Block.BLOCKS[id] : Block.BLOCKS[1];
  }
}
