package local.luke.power.offhand.mixin;

import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_277.class, class_506.class, class_407.class, class_196.class, class_337.class})
public abstract class OffhandContainerMixin extends Container {
  @Inject(method="<init>*", at=@At("RETURN"), remap=false)
  private void power$appendOffhand(CallbackInfo ci) {
    PlayerInventory inventory = OffhandContainers.owner(this);
    // Remote container protocols retain their native layout.
    if (inventory == null || inventory.player.world != null && inventory.player.world.isRemote
        || OffhandContainers.slot(this, inventory) != null) return;
    method_2079(new OffhandNativeSlot(inventory, OffhandContainers.hotbarY(this, inventory)));
  }
}
