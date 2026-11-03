package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import local.luke.power.gameplay.TrapdoorClimbing;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class TrapdoorClimbingCollisionMixin {
  @WrapOperation(method = "move", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_190(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
  private List<Box> power$continueLadderWall(World world, Entity entity, Box bounds, Operation<List<Box>> original) {
    return TrapdoorClimbing.movementCollisions(entity, original.call(world, entity, bounds));
  }
}
