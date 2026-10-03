package local.luke.power.building.mixin;

import local.luke.power.building.hotbar.HotbarOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HotbarHudMixin {
  @Shadow private Minecraft minecraft;

  @Inject(method = "render", at = @At("TAIL"))
  private void power$overlay(float delta, boolean screen, int x, int y, CallbackInfo ci) {
    HotbarOverlay.render(minecraft);
  }
}
