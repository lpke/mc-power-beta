package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import net.minecraft.class_277;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Container;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_277.class)
public abstract class OffhandContainerMixin extends Container {
  @Inject(method="<init>(Lnet/minecraft/entity/player/PlayerInventory;Z)V", at=@At("RETURN"))
  private void power$appendOffhand(PlayerInventory inventory, boolean local, CallbackInfo ci) {
    // Remote container protocols retain their native layout.
    if (inventory.player.world != null && inventory.player.world.isRemote) return;
    method_2079(new OffhandNativeSlot(inventory));
  }
}
