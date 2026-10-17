package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.ui.MenuPanorama;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native world/pack lists keep their scrolling and hit targets over the menu panorama. */
@Mixin(EntryListWidget.class)
public abstract class MenuListBackgroundMixin {
  @Shadow private Minecraft minecraft;
  @Shadow private int width;
  @Shadow private int height;
  @Shadow private int top;
  @Shadow private int bottom;
  @WrapMethod(method = "render")
  private void power$clipFrame(int x, int y, float delta, Operation<Void> original) {
    if (!MenuPanorama.enabled(minecraft)) { original.call(x, y, delta); return; }
    GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    try { original.call(x, y, delta); }
    finally { GL11.glPopAttrib(); }
  }
  @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V", ordinal = 0))
  private void power$transparentList(Tessellator tessellator, Operation<Void> original) {
    if (!MenuPanorama.enabled(minecraft)) { original.call(tessellator); return; }
    GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    try { GL11.glColorMask(false, false, false, false); GL11.glDepthMask(false); original.call(tessellator); }
    finally { GL11.glPopAttrib(); }
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    int y1 = minecraft.displayHeight - bottom * minecraft.displayHeight / height;
    int y2 = minecraft.displayHeight - top * minecraft.displayHeight / height;
    GL11.glScissor(0, y1, minecraft.displayWidth, Math.max(0, y2 - y1));
  }
  @Inject(method = "renderBackground(IIII)V", at = @At("HEAD"))
  private void power$nativeBorders(int top, int bottom, int firstAlpha, int lastAlpha, CallbackInfo ci) {
    if (MenuPanorama.enabled(minecraft)) GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }
}
