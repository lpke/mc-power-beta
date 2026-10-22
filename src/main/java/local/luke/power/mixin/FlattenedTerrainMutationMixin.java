package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.WaterTerrain;
import net.minecraft.class_43;
import net.modificationstation.stationapi.api.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** FlattenedChunk replaces both native setters and exposes two independent state setters. */
@Mixin(targets = "net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk", remap = false)
public class FlattenedTerrainMutationMixin {
  @WrapMethod(method = "setBlockState(IIILnet/modificationstation/stationapi/api/block/BlockState;I)Lnet/modificationstation/stationapi/api/block/BlockState;")
  private BlockState power$stateAndMeta(int x, int y, int z, BlockState state, int meta, Operation<BlockState> original) {
    var world = ((class_43)(Object)this).field_956;
    boolean entered = WaterTerrain.beginMutation(world);
    try { return original.call(x, y, z, state, meta); }
    finally { WaterTerrain.endMutation(world, entered); }
  }

  @WrapMethod(method = "setBlockState(IIILnet/modificationstation/stationapi/api/block/BlockState;)Lnet/modificationstation/stationapi/api/block/BlockState;")
  private BlockState power$state(int x, int y, int z, BlockState state, Operation<BlockState> original) {
    var world = ((class_43)(Object)this).field_956;
    boolean entered = WaterTerrain.beginMutation(world);
    try { return original.call(x, y, z, state); }
    finally { WaterTerrain.endMutation(world, entered); }
  }
}
