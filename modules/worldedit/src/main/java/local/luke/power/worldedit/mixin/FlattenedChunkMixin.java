package local.luke.power.worldedit.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import local.luke.power.worldedit.EditScope;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** StationAPI replaces vanilla chunk writes, including placement and break callbacks. */
@Pseudo
@Mixin(targets = "net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk", remap = false)
public abstract class FlattenedChunkMixin {
  // These API method names do not use Minecraft mappings. Coerce keeps this
  // adapter optional without introducing a compile dependency on StationAPI.
  @WrapWithCondition(method = "setBlockState",
      at = @At(value = "INVOKE", target = "onBlockPlaced", remap = false), remap = false)
  private boolean worldedit$place(@Coerce Object block, World world, int x, int y, int z,
      @Coerce Object previousState) {
    return !EditScope.active(world);
  }

  @WrapWithCondition(method = "setBlockState",
      at = @At(value = "INVOKE", target = "onStateReplaced", remap = false), remap = false)
  private boolean worldedit$remove(@Coerce Object state, World world, @Coerce Object position,
      @Coerce Object replacementState) {
    return !EditScope.active(world);
  }
}
