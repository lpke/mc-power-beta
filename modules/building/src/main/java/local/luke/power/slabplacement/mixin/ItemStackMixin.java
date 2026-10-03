package local.luke.power.slabplacement.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.slabplacement.*;
import net.minecraft.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @WrapOperation(
      method = "method_701",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/Item;method_444(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;IIII)Z"))
  private boolean merge(
      Item item,
      ItemStack stack,
      PlayerEntity player,
      World world,
      int x,
      int y,
      int z,
      int face,
      Operation<Boolean> original) {
    Minecraft mc = MinecraftAccessor.instance();
    if (mc != null && player == mc.player && mc.world == world && !Compatibility.freecam()) {
      if (local.luke.power.building.SlabPairs.active(mc, stack))
        return local.luke.power.building.SlabPairs.place(mc, stack, x, y, z, face);
      int[] target = SlabMerge.target(world, stack, x, y, z, face);
      if (target != null) return SlabMerge.complete(world, stack, target);
    }
    return original.call(item, stack, player, world, x, y, z, face);
  }
}
