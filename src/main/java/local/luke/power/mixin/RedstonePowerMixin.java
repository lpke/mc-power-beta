package local.luke.power.mixin;

import local.luke.power.visual.RedstonePowerRenderer;
import net.minecraft.block.Block;
import net.minecraft.class_13;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_13.class)
public abstract class RedstonePowerMixin {
  @Shadow private BlockView field_82;
  @Shadow private int field_83;

  @Inject(method = "method_71", at = @At("HEAD"), cancellable = true)
  private void power$redstone(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> ci) {
    // Leave the breaking overlay on its normal render path.
    if (field_83 < 0 && RedstonePowerRenderer.render(field_82, block, x, y, z)) ci.setReturnValue(true);
  }
}
