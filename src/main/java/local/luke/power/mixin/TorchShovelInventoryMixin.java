package local.luke.power.mixin;

import local.luke.power.gameplay.TorchShovel;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class TorchShovelInventoryMixin {
  @Shadow public PlayerEntity player;
  @Inject(method = "method_674", at = @At("HEAD"), cancellable = true)
  private void power$nativeSpeed(Block block, CallbackInfoReturnable<Float> cir) {
    int slot = TorchShovel.slot(player, block);
    if (slot >= 0) cir.setReturnValue(player.inventory.main[slot].method_708(block));
  }
}
