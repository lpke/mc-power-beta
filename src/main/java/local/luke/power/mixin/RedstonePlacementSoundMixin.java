package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.audio.AudioConfig;
import net.minecraft.block.Block;
import net.minecraft.class_183;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_183.class)
public abstract class RedstonePlacementSoundMixin {
  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_229(IIII)Z"))
  private boolean power$placementSound(World world, int x, int y, int z, int block, Operation<Boolean> original) {
    boolean placed = original.call(world, x, y, z, block);
    if (placed && world.getBlockId(x, y, z) == block && AudioConfig.current().redstonePlacementSound) {
      var sound = Block.STONE.field_1926;
      world.method_150(x + .5, y + .5, z + .5,
          sound.method_1978(), (sound.method_1976() + 1) / 2, sound.method_1977() * .8f);
    }
    return placed;
  }
}
