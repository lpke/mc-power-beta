package local.luke.power.mixin;

import java.util.Random;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.FluidSounds;
import net.minecraft.class_385;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_385.class)
public abstract class LavaPopSoundMixin {
  @Inject(method = "method_1617", at = @At("TAIL"))
  private void power$ambient(World world, int x, int y, int z, Random random, CallbackInfo ci) {
    FluidSounds.lavaAmbient(world, x, y, z);
  }
  @WrapOperation(method = "method_1617", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;addParticle(Ljava/lang/String;DDDDDD)V"))
  private void power$exposedLavaPop(World world, String particle, double x, double y, double z,
      double vx, double vy, double vz, Operation<Void> original,
      World displayWorld, int blockX, int blockY, int blockZ, Random random) {
    original.call(world, particle, x, y, z, vx, vy, vz);
    if (particle.equals("lava")) FluidSounds.lavaPop(world, x, blockY + 1, z);
  }
}
