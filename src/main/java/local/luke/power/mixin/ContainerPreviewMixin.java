package local.luke.power.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class ContainerPreviewMixin extends DrawableHelper {
  @Shadow private Minecraft minecraft;
  @Inject(method = "render", at = @At("TAIL"))
  private void power$preview(float delta, boolean focused, int mx, int my, CallbackInfo ci) {
    local.luke.power.inventory.ContainerPreview.render(minecraft);
  }
}
