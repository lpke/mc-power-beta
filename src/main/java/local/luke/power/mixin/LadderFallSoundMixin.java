package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.*;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LadderFallSoundMixin {
  @WrapOperation(method = "method_1389", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$ladder(World world, Entity entity, String sound, float volume, float pitch, Operation<Void> original) {
    if (LadderSounds.at(world, entity.x, entity.boundingBox.minY - .2f, entity.z))
      SoundCategoryScope.run(FluidSounds.entityCategory(entity), () -> original.call(world, entity, "power_beta:block.ladder.fall", volume, pitch));
    else original.call(world, entity, sound, volume, pitch);
  }
}
