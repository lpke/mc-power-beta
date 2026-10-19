package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.class_113;
import net.minecraft.class_212;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native boat interaction consumes use before Beta's unconditional held-item fallback. */
@Mixin(Minecraft.class)
public class BoatInteractionMixin {
  @WrapOperation(
      method = "method_2107",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/InteractionManager;method_1712(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)Z"))
  private boolean power$boatInteraction(
      InteractionManager interaction,
      PlayerEntity player,
      World world,
      ItemStack held,
      Operation<Boolean> original) {
    Minecraft mc = (Minecraft) (Object) this;
    if (held.itemId == 333
        && mc.field_2823 != null
        && mc.field_2823.field_1983 == class_212.ENTITY
        && mc.field_2823.field_1989 instanceof class_113) return false;
    return original.call(interaction, player, world, held);
  }
}
