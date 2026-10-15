package local.luke.power.mixin;

import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.PlayerMode;
import local.luke.power.rules.*;
import net.minecraft.Item;
import net.minecraft.block.Block;
import net.minecraft.class_132;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_132.class)
public class TntIgnitionMixin {
  @Inject(method = "method_1608", at = @At("HEAD"), cancellable = true)
  private void power$ignite(World world, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
    if (world.isRemote || !WorldRules.enabled(world.method_262(), GameRule.TNT_EXPLODES)
        || !GameplayConfig.current().flintIgnitesTnt
        || player instanceof PlayerMode mode && mode.power$spectator()) return;
    ItemStack stack = player.method_502();
    if (stack == null || stack.itemId != Item.field_472.id || world.getBlockId(x, y, z) != Block.field_1887.id) return;
    if (!world.method_229(x, y, z, 0)) return;
    Block.field_1887.method_1612(world, x, y, z, 1);
    if (!(player instanceof PlayerMode mode && mode.power$creative())) stack.damage(1, player);
    cir.setReturnValue(true);
  }
}
