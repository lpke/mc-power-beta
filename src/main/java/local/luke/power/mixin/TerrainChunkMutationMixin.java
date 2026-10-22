package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.render.WaterTerrain;
import net.minecraft.class_43;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(class_43.class)
public class TerrainChunkMutationMixin {
  @Shadow public World field_956;

  @WrapMethod(method = "method_861")
  private boolean power$stateAndMeta(int x, int y, int z, int id, int meta, Operation<Boolean> original) {
    boolean entered = WaterTerrain.beginMutation(field_956);
    try { return original.call(x, y, z, id, meta); }
    finally { WaterTerrain.endMutation(field_956, entered); }
  }

  @WrapMethod(method = "method_860")
  private boolean power$state(int x, int y, int z, int id, Operation<Boolean> original) {
    boolean entered = WaterTerrain.beginMutation(field_956);
    try { return original.call(x, y, z, id); }
    finally { WaterTerrain.endMutation(field_956, entered); }
  }
}
