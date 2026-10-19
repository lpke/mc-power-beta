package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.ExtraSounds;
import net.minecraft.class_380;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_380.class)
public abstract class SeedPlantingSoundMixin {
  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_229(IIII)Z"))
  private boolean power$plant(World world, int x, int y, int z, int block, Operation<Boolean> original) {
    boolean placed = original.call(world, x, y, z, block);
    if (placed && world.getBlockId(x, y, z) == block && AudioConfig.current().seedPlantingSounds)
      ExtraSounds.play(world, x + .5, y + .5, z + .5, "item.crop.plant");
    return placed;
  }
}
