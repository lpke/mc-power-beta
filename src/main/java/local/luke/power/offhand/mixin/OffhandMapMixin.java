package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.offhand.Offhand;
import net.minecraft.class_198;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Map tracking includes the enabled offhand without changing native inventory searches. */
@Mixin(class_198.class)
public class OffhandMapMixin {
  @WrapOperation(method = "method_667", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/entity/player/PlayerInventory;method_681(Lnet/minecraft/item/ItemStack;)Z"))
  private boolean power$heldMap(PlayerInventory inventory, ItemStack map, Operation<Boolean> original) {
    if (original.call(inventory, map)) return true;
    ItemStack off = Offhand.enabled(inventory.player) ? Offhand.get(inventory) : null;
    return off != null && off.equals(map);
  }
}
