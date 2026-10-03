package local.luke.power.creative.inventory.mixin.common;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
  @Shadow public int count;
  @Shadow private int damage;

  @WrapMethod(method = "useOnBlock")
  private boolean creative$useOnBlock(
      PlayerEntity player,
      Level level,
      int x,
      int y,
      int z,
      int side,
      Operation<Boolean> original) {
    boolean preserve = player.creative_isCreative();
    int oldCount = count, oldDamage = damage;
    try {
      return original.call(player, level, x, y, z, side);
    } finally {
      if (preserve) {
        count = oldCount;
        damage = oldDamage;
      }
    }
  }

  @WrapMethod(method = "use")
  private ItemStack creative$use(Level level, PlayerEntity player, Operation<ItemStack> original) {
    boolean preserve = player.creative_isCreative();
    int oldCount = count, oldDamage = damage;
    try {
      return original.call(level, player);
    } finally {
      if (preserve) {
        count = oldCount;
        damage = oldDamage;
      }
    }
  }

  @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
  private void creative_applyDamage(int damage, Entity entity, CallbackInfo info) {
    if (entity instanceof PlayerEntity player && player.creative_isCreative()) info.cancel();
  }
}
