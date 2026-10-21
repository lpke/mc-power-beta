package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.offhand.OffhandUse;
import local.luke.power.offhand.OffhandSlotClick;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InteractionManager.class)
public class OffhandNativeUseMixin {
  @Shadow @Final protected Minecraft minecraft;
  @WrapMethod(method="clickSlot")
  private ItemStack power$sideSlot(int syncId,int slotId,int button,boolean shift,PlayerEntity player,Operation<ItemStack> original) {
    int nativeSlot=OffhandSlotClick.nativeSlot(minecraft,slotId);
    return nativeSlot==-1 ? null : original.call(syncId,nativeSlot,button,shift,player);
  }
  @WrapOperation(method="method_1713", at=@At(value="INVOKE", target="Lnet/minecraft/block/Block;method_1608(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/PlayerEntity;)Z"))
  private boolean power$blockHand(Block block, World world, int x, int y, int z, PlayerEntity player, Operation<Boolean> original) {
    return !OffhandUse.skipBlock(player,block) && original.call(block, world, x, y, z, player);
  }
}
