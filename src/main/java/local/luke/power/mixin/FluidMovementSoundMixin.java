package local.luke.power.mixin;

import java.util.Random;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.audio.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class FluidMovementSoundMixin {
  @Shadow protected Random random;
  @Unique private float power$soundDistance, power$nextSwimSound = 1;
  @Unique private boolean power$soundMoving;
  @Unique private double power$soundMoveY;
  @WrapMethod(method = "move")
  private void power$swimMovement(double dx, double dy, double dz, Operation<Void> original) {
    Entity entity = (Entity)(Object)this;
    boolean moving = FluidSounds.movement(entity); double x = entity.x, y = entity.boundingBox.minY, z = entity.z;
    boolean priorMoving = power$soundMoving; double priorY = power$soundMoveY;
    power$soundMoving = moving; power$soundMoveY = y;
    try { original.call(dx, dy, dz); }
    finally { power$soundMoving = priorMoving; power$soundMoveY = priorY; }
    if (!moving || !FluidSounds.movement(entity)) { power$soundDistance = 0; power$nextSwimSound = 1; return; }
    int stepBlock = FluidSounds.stepBlock(entity);
    double vertical = entity.boundingBox.minY - y;
    power$soundDistance += FluidSoundMath.distance(entity.x - x,
        stepBlock == net.minecraft.block.Block.field_1854.id ? vertical : 0, entity.z - z);
    if (power$soundDistance > power$nextSwimSound && stepBlock > 0) {
      power$nextSwimSound = (float)FluidSoundMath.nextStep(power$soundDistance);
      if (FluidSounds.swimStep(entity, stepBlock, vertical)) FluidSounds.swim(entity, random);
    }
  }
  @WrapOperation(method = "move", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
  private void power$waterStep(World world, Entity entity, String sound, float volume, float pitch, Operation<Void> original) {
    if (power$soundMoving && sound.startsWith("step.") && FluidSounds.movement(entity)
        && FluidSounds.swimStep(entity, FluidSounds.stepBlock(entity), entity.boundingBox.minY - power$soundMoveY)) return;
    original.call(world, entity, sound, volume, pitch);
  }
}
