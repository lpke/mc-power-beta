package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.List;
import local.luke.power.gameplay.CollisionCandidates;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(World.class)
public class ItemCollisionMixin {
  @WrapOperation(method = "method_190", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
  private List<?> power$collisionCandidates(World world, Entity entity, Box box, Operation<List<?>> original) {
    if (!local.luke.power.gameplay.GameplayConfig.current().fastItemCollisions || world.isRemote || !CollisionCandidates.simple(entity)) return original.call(world, entity, box);
    CollisionCandidates.begin();
    try { return original.call(world, entity, box); }
    finally { CollisionCandidates.end(); }
  }
}
