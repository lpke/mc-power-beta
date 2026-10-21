package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.offhand.OffhandUse;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InteractionManager.class)
public class OffhandNativeUseMixin {
  @WrapOperation(method="method_1713", at=@At(value="INVOKE", target="Lnet/minecraft/block/Block;method_1608(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/PlayerEntity;)Z"))
  private boolean power$blockHand(Block block, World world, int x, int y, int z, PlayerEntity player, Operation<Boolean> original) {
    return !OffhandUse.skipBlock(player,block) && original.call(block, world, x, y, z, player);
  }
}
