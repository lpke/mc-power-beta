package local.luke.power.mixin;

import local.luke.power.gameplay.AxesSlashLeaves;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class AxesSlashLeavesInventoryMixin {
  @Shadow public PlayerEntity player;
  @Inject(method = "method_674", at = @At("RETURN"), cancellable = true)
  private void power$leafSpeed(Block block, CallbackInfoReturnable<Float> cir) {
    cir.setReturnValue(AxesSlashLeaves.speed(player, block, cir.getReturnValueF()));
  }
}
