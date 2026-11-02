package local.luke.power.mixin;

import local.luke.power.bow.ModernBow;
import net.minecraft.class_367;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_367.class)
public class ModernBowItemMixin {
  @Inject(method="method_451", at=@At("HEAD"), cancellable=true)
  private void power$draw(ItemStack stack, World world, PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
    if (!ModernBow.enabled() || world.isRemote) return;
    ModernBow.begin(player, stack);
    cir.setReturnValue(stack);
  }
}
