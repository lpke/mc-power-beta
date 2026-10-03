package local.luke.building.validation.mixin;

import local.luke.building.validation.ItemUseFault;
import net.minecraft.Item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemUseFaultMixin {
  @Inject(method = "method_444", at = @At("HEAD"), cancellable = true)
  private void validate$fault(
      ItemStack stack,
      PlayerEntity player,
      World world,
      int x,
      int y,
      int z,
      int side,
      CallbackInfoReturnable<Boolean> ci) {
    if (ItemUseFault.enabled) ci.setReturnValue(ItemUseFault.use(stack, player, world));
  }
}
