package local.luke.creative.mixin;

import local.luke.creative.*;
import net.minecraft.client.ClientInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientInteractionManager.class)
public abstract class ReachMixin {
  @Shadow @Final protected Minecraft minecraft;

  @Inject(
      method = {"playerDigBlock", "digBlock"},
      at = @At("HEAD"),
      cancellable = true)
  private void lpke$dig(int x, int y, int z, int face, CallbackInfo ci) {
    if (Modes.spectator(minecraft.player)) ci.cancel();
  }

  @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
  private void lpke$use(
      PlayerEntity p,
      Level w,
      ItemStack stack,
      int x,
      int y,
      int z,
      int face,
      CallbackInfoReturnable<Boolean> ci) {
    if (Modes.spectator(p)) ci.setReturnValue(false);
  }

  @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
  private void lpke$use(
      PlayerEntity p, Level w, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
    if (Modes.spectator(p)) ci.setReturnValue(false);
  }

  @Inject(
      method = {"attack", "processInteraction"},
      at = @At("HEAD"),
      cancellable = true)
  private void lpke$entity(PlayerEntity p, Entity target, CallbackInfo ci) {
    if (Modes.spectator(p)) ci.cancel();
  }
}
