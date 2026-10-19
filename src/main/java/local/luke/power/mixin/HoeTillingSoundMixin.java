package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.ExtraSounds;
import net.minecraft.class_249;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_249.class)
public abstract class HoeTillingSoundMixin {
  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_150(DDDLjava/lang/String;FF)V"))
  private void power$nativeSound(World world, double x, double y, double z, String sound,
      float volume, float pitch, Operation<Void> original) {
    if (world.isRemote || !AudioConfig.current().hoeTillingSounds)
      original.call(world, x, y, z, sound, volume, pitch);
  }

  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_229(IIII)Z"))
  private boolean power$till(World world, int x, int y, int z, int block, Operation<Boolean> original) {
    boolean tilled = original.call(world, x, y, z, block);
    if (tilled && world.getBlockId(x, y, z) == block && AudioConfig.current().hoeTillingSounds)
      ExtraSounds.play(world, x + .5, y + .5, z + .5, "item.hoe.till");
    return tilled;
  }
}
