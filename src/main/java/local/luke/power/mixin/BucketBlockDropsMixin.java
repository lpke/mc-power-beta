package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.block.Block;
import net.minecraft.class_220;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_220.class)
public abstract class BucketBlockDropsMixin {
  @WrapOperation(method = "method_451", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_201(IIIII)Z"))
  private boolean power$dropReplacedBlock(World world, int x, int y, int z, int fluid,
      int metadata, Operation<Boolean> original) {
    if (world.isRemote || !GameplayConfig.current().bucketBlockDrops)
      return original.call(world, x, y, z, fluid, metadata);
    int oldId = world.getBlockId(x, y, z);
    Block replaced = Block.BLOCKS[oldId];
    var material = world.method_1779(x, y, z);
    int oldMetadata = world.method_1778(x, y, z);
    boolean placed = original.call(world, x, y, z, fluid, metadata);
    // Drop only after native placement succeeds. Nether evaporation never reaches this call.
    if (placed && oldId != fluid && replaced != null && !material.method_893()
        && !material.method_905() && world.getBlockId(x, y, z) == fluid)
      replaced.method_1592(world, x, y, z, oldMetadata);
    return placed;
  }
}
