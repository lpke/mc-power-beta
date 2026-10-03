package local.luke.power.flexible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.flexible.*;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InteractionManager.class)
public abstract class InteractionManagerMixin {
  @Shadow protected Minecraft minecraft;

  @WrapOperation(
      method = "method_1713",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/block/Block;method_1608(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/PlayerEntity;)Z"))
  private boolean skipInteraction(
      Block block,
      World world,
      int x,
      int y,
      int z,
      PlayerEntity player,
      Operation<Boolean> original) {
    if (player == minecraft.player
        && FlexiblePlacement.handles(minecraft, player.inventory.getSelectedItem())) return false;
    return original.call(block, world, x, y, z, player);
  }

  @WrapOperation(
      method = "method_1713",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/item/ItemStack;method_701(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;IIII)Z"))
  private boolean place(
      ItemStack stack,
      PlayerEntity player,
      World world,
      int x,
      int y,
      int z,
      int face,
      Operation<Boolean> original) {
    if (player != minecraft.player || !FlexiblePlacement.handles(minecraft, stack))
      return original.call(stack, player, world, x, y, z, face);
    PlacementPlan p = FlexiblePlacement.plan(minecraft, stack, x, y, z, face);
    if (p == null || !p.valid()) return false;
    Orientation.Request previous =
        Orientation.begin(
            new Orientation.Request(
                NativePlacement.block(stack), p.destination(), p.facing(), p.reverse()));
    try {
      return original.call(
          stack,
          player,
          world,
          p.clicked().x(),
          p.clicked().y(),
          p.clicked().z(),
          p.face().ordinal());
    } finally {
      Orientation.restore(previous);
    }
  }
}
