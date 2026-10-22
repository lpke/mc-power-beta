package local.luke.power.mixin;

import local.luke.power.death.KeepInventory;
import net.minecraft.class_277;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Death closure keeps held items and the player's four crafting inputs for respawn. */
@Mixin({Container.class, class_277.class})
public abstract class KeptInventoryContainerMixin {
  @Inject(method = "method_2087", at = @At("HEAD"), cancellable = true)
  private void power$keepHeld(PlayerEntity player, CallbackInfo ci) {
    if (KeepInventory.kept(player) || player instanceof local.luke.power.input.PlayerMode mode && mode.power$spectator()) ci.cancel();
  }
}
