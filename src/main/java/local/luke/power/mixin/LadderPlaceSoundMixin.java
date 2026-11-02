package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.LadderSounds;
import net.minecraft.class_533;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_533.class)
public abstract class LadderPlaceSoundMixin {
  @WrapOperation(method = "method_444", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_150(DDDLjava/lang/String;FF)V"))
  private void power$ladder(World world, double x, double y, double z, String sound, float volume,
      float pitch, Operation<Void> original) {
    original.call(world, x, y, z, LadderSounds.at(world, x, y, z) ? "power_beta:block.ladder.place" : sound, volume, pitch);
  }
}
