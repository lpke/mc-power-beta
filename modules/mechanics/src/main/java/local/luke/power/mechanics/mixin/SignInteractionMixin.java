package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class SignInteractionMixin {
  @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
  private void power$editSign(World world, int x, int y, int z, PlayerEntity player,
      CallbackInfoReturnable<Boolean> cir) {
    if (!Config.config.INTERACTIVE_BLOCK_CONFIG.allowEditingSigns || player.isSneaking()) return;
    Block block = (Block) (Object) this;
    if (block != Block.SIGN && block != Block.WALL_SIGN) return;
    if (world.getBlockEntity(x, y, z) instanceof SignBlockEntity sign) {
      if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER)
        sign.setEditable(true);
      player.openEditSignScreen(sign);
      cir.setReturnValue(true);
    }
  }
}
