package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.gameplay.GameplayConfig;
import net.minecraft.class_113;
import net.minecraft.class_483;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BoatItem's placement path, shared by native and improved boat movement. */
@Mixin(class_483.class)
public abstract class BoatPlacementMixin {
  @WrapOperation(method = "method_451", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;method_210(Lnet/minecraft/entity/Entity;)Z"))
  private boolean power$facePlacement(World world, Entity boat, Operation<Boolean> spawn,
      ItemStack stack, World placementWorld, PlayerEntity player) {
    if (!world.isRemote && boat instanceof class_113 && GameplayConfig.current().boatPlacementFacing) {
      // Beta's boat model and movement heading are 90 degrees behind player yaw.
      boat.yaw = boat.prevYaw = player.yaw % 360.0f - 90.0f;
    }
    return spawn.call(world, boat);
  }
}
