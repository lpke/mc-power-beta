package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Random;
import local.luke.power.audio.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beta clears fire on water contact before its native movement fizz can see it. */
@Mixin(Entity.class)
public abstract class PlayerFireExtinguishMixin {
  @Shadow protected boolean fireImmune;
  @Unique private static final Random power$extinguishRandom = new Random();

  @Inject(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;fire:I",
      opcode = Opcodes.PUTFIELD, ordinal = 0))
  private void power$waterExtinguish(CallbackInfo ci) {
    Entity entity = (Entity)(Object)this;
    // This is only the water branch, after native splash particles, before fire = 0.
    if (entity.fire > 0 && !fireImmune && power$enabled(entity))
      FluidSounds.entitySound(entity, "entity.generic.extinguish_fire", .7f,
          1.6f + (power$extinguishRandom.nextFloat() - power$extinguishRandom.nextFloat()) * .4f);
  }

  @WrapOperation(method = "move", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$nativeWaterFizz(World world, Entity entity, String sound, float volume, float pitch,
      Operation<Void> original) {
    // Already-wet ignition can reach the original fizz. Reuse it instead of adding a second event.
    // Rain, other entities and multiplayer keep their native resource and random draws.
    if (sound.equals("random.fizz") && !fireImmune && entity.method_1334() && power$enabled(entity))
      FluidSounds.entitySound(entity, "entity.generic.extinguish_fire", volume, pitch);
    else original.call(world, entity, sound, volume, pitch);
  }

  @Unique private static boolean power$enabled(Entity entity) {
    return AudioConfig.current().playerFireExtinguish && entity instanceof PlayerEntity
        && FluidSounds.localWorld(entity.world);
  }
}
