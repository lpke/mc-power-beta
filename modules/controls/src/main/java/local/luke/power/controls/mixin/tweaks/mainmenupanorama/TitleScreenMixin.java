package local.luke.power.controls.mixin.tweaks.mainmenupanorama;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    @Shadow private float ticks;
    @Unique private Tessellator tessellator;
    @Unique private boolean power$panoramaEnabled() {
        return ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.enablePanorma
            && !local.luke.power.input.PanoramaCatalog.folder(ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.panoramaFolder).isEmpty();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    public void moveVersionText(TitleScreen instance, TextRenderer textRenderer, String text, int x, int y, int color, Operation<Void> original) {
        if (power$panoramaEnabled()) {
            textRenderer.drawWithShadow(text, x, this.height - 10, Color.white.getRGB());
        } else {
            original.call(instance, textRenderer, text, x, y, color);
        }
    }


    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V"))
    public void cancelDefaultBackgroundRendering(TitleScreen instance, Operation<Void> original) {
        // Redirect to nothing
        if (!power$panoramaEnabled()) {
            original.call(instance);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V", shift = At.Shift.AFTER))
    public void redirectBackgroundRendering(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (power$panoramaEnabled()) {
            this.tessellator = Tessellator.INSTANCE;
            GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
            drawPanorama(delta);
            fillGradient(0, 0, this.width, this.height, -2130706433, 16777215);
            fillGradient(0, 0, this.width, this.height, 0, Integer.MIN_VALUE);
        }
    }

    @Unique
    private void drawPanorama(float delta) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluPerspective(90.0F, (float) this.minecraft.displayWidth / Math.max(1, this.minecraft.displayHeight), 0.05F, 10.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        String folder = local.luke.power.input.PanoramaCatalog.folder(ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.panoramaFolder);
        int var5 = ControlFeatures.USER_INTERFACE_CONFIG.panoramaConfig.blurBackground ? 4 : 1;

        for (int var6 = 0; var6 < var5 * var5; ++var6) {
            GL11.glPushMatrix();
            float var7 = ((float) (var6 % var5) / (float) var5 - 0.5F) / 64.0F;
            float var8 = ((float) (var6 / var5) / (float) var5 - 0.5F) / 64.0F;
            if (var5 > 1) GL11.glTranslatef(var7, var8, 0F);
            GL11.glRotatef(MathHelper.sin((this.ticks + delta) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F); // Up & Down Waving
            GL11.glRotatef(-(this.ticks + delta) * 0.1F, 0.0F, 1.0F, 0.0F); // Horizontal Rotation

            for (int rotation = 0; rotation < 6; ++rotation) {
                GL11.glPushMatrix();

                switch (rotation) {
                    case 1 -> GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                    case 2 -> GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                    case 3 -> GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                    case 4 -> GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                    case 5 -> GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                GL11.glBindTexture(
                        GL11.GL_TEXTURE_2D,
                        this.minecraft.textureManager.getTextureId("/assets/power_controls/textures/" + folder + "/panorama" + rotation + ".png")
                );
                tessellator.startQuads();
                tessellator.color(16777215, 255 / (var6 + 1));
                tessellator.vertex(-1.0D, -1.0D, 1.0D, 0.0F, 0.0F);
                tessellator.vertex(1.0D, -1.0D, 1.0D, 1.0F, 0.0F);
                tessellator.vertex(1.0D, 1.0D, 1.0D, 1.0F, 1.0F);
                tessellator.vertex(-1.0D, 1.0D, 1.0D, 0.0F, 1.0F);
                tessellator.draw();
                GL11.glPopMatrix();
            }

            GL11.glPopMatrix();
            GL11.glColorMask(true, true, true, false);
        }

        tessellator.setOffset(0.0D, 0.0D, 0.0D);
        GL11.glColorMask(true, true, true, true);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
}