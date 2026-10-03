package local.luke.flexible.mixin;

import local.luke.flexible.PlacementOverlay;
import net.minecraft.class_27;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
  @Inject(method = "method_1554", at = @At("TAIL"))
  private void overlay(
      PlayerEntity player, class_27 hit, int pass, ItemStack stack, float delta, CallbackInfo ci) {
    if (pass == 0) PlacementOverlay.render(player, hit, delta);
  }
}
