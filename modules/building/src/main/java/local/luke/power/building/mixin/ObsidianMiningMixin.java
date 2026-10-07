package local.luke.power.building.mixin;

import local.luke.power.building.ObsidianMining;
import local.luke.power.building.config.Config;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.modificationstation.stationapi.api.block.AbstractBlockState", remap = false)
public abstract class ObsidianMiningMixin {
  @Shadow public abstract Block getBlock();

  @Inject(method = "calcBlockBreakingDelta", at = @At("RETURN"), cancellable = true)
  private void power$obsidianSpeed(PlayerEntity player, @Coerce Object world, @Coerce Object position,
      CallbackInfoReturnable<Float> cir) {
    // StationAPI replaces the vanilla block-strength path used by the interaction manager.
    if (getBlock() != Block.field_1890 || player.world.isRemote || cir.getReturnValueF() >= 1) return;
    int speed = Config.current().obsidianBreakingSpeed;
    if (speed == 0) return;
    ItemStack held = player.method_502();
    if (held != null && held.itemId == 278 && player.method_514(getBlock()))
      cir.setReturnValue(ObsidianMining.progress(cir.getReturnValueF(), speed));
  }
}
