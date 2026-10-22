package local.luke.power.mixin;

import local.luke.power.input.PlayerMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public abstract class SpectatorSlotMixin {
  @Inject(method="onSlotClick",at=@At("HEAD"),cancellable=true)
  private void power$blockOutsideDrops(int slot,int button,boolean shift,PlayerEntity player,CallbackInfoReturnable<ItemStack> ci) {
    if (slot == -999 && player instanceof PlayerMode mode && mode.power$spectator()) ci.setReturnValue(null);
  }
}
