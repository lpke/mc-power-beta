package me.lukiiy.oldLogo.mixin;

import me.lukiiy.oldLogo.LogoEffectRandomizer;
import me.lukiiy.oldLogo.OldLogo;
import net.minecraft.block.Block;
import net.minecraft.class_564;
import net.minecraft.class_13;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(value = TitleScreen.class, priority = 56676)
public class TitleScreenMixin {
    @Shadow @Final private static Random RANDOM;

    @Unique
    private LogoEffectRandomizer[][] logoEffects;

    @Redirect(method = "render(IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(IIIIII)V"))
    private void oldLogo$cancelOriginal(TitleScreen instance, int x, int y, int u, int v, int w, int h) {
        if (w == 155 && h == 44) return;

        instance.drawTexture(x, y, u, v, w, h);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void oldLogo$tick(CallbackInfo ci) {
        if (logoEffects == null || !OldLogo.animation.get()) return;

        for (LogoEffectRandomizer[] logoEffect : logoEffects) {
            for (LogoEffectRandomizer logoEffectRandomizer : logoEffect) logoEffectRandomizer.tick();
        }
    }

    @Inject(method = "render(IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground()V", shift = At.Shift.AFTER))
    private void oldLogo$render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft minecraft = OldLogo.minecraft;

        String[] logo = OldLogo.logo.get();
        if (logo == null || logo.length == 0) return;

        if (logoEffects == null) {
            logoEffects = new LogoEffectRandomizer[logo[0].length()][logo.length];

            for (int x = 0; x < logoEffects.length; x++) {
                for (int y = 0; y < logoEffects[x].length; y++) logoEffects[x][y] = new LogoEffectRandomizer(RANDOM, x, y, OldLogo.animationSpeed.get().floatValue());
            }
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        class_564 scaledRes = new class_564(minecraft.options, minecraft.displayWidth, minecraft.displayHeight);
        int viewHeight = 120 * scaledRes.field_2391;

        GLU.gluPerspective(70, (float) minecraft.displayWidth / (float) viewHeight, .05f, 100);
        GL11.glViewport(0, minecraft.displayHeight - viewHeight, minecraft.displayWidth, viewHeight);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        class_13 blockRenderer = new class_13();

        for (int pass = 0; pass < 3; pass++) {
            if (pass == 0 && !OldLogo.shadow.get()) continue;

            GL11.glPushMatrix();
            GL11.glTranslatef(.4f, .6f, -13f);

            if (pass == 0) {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glTranslatef(0, -.4f, 0);
                GL11.glScalef(.98f, 1, 1);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else if (pass == 1) {
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            } else {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            }

            GL11.glScalef(1, -1, 1);

            float[] rot = OldLogo.logoRotation.get();
            GL11.glRotatef(rot[0], 1f, 0f, 0f); // X
            GL11.glRotatef(rot[1], 0f, 1f, 0f); // Y
            GL11.glRotatef(rot[2], 0f, 0f, 1f); // Z

            GL11.glScalef(.89f, 1, .4f);
            GL11.glTranslatef(-logo[0].length() * .5f, -logo.length * .5f, 0);

            if (pass == 0) GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/title/black.png"));
            else GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/terrain.png"));

            OldLogo.isLogoRendering = true;

            for (int y = 0; y < logo.length; y++) {
                for (int x = 0; x < logo[y].length(); x++) {
                    Integer id = OldLogo.blockMap.get().get(logo[y].charAt(x));
                    if (id == null || id == 0) continue;

                    Block block = id > 0 && id < Block.BLOCKS.length ? Block.BLOCKS[id] : null;
                    if (block == null) continue;

                    GL11.glPushMatrix();
                    LogoEffectRandomizer effect = logoEffects[x][y];

                    float z = 0.0f;
                    float scale = 1;

                    if (OldLogo.animation.get()) z = (float) (effect.current + (effect.target - effect.current) * delta);
                    if (pass == 0) {
                        scale = z * .04f + 1;
                        z = 0;
                    }

                    GL11.glTranslatef(x, y, z);
                    GL11.glScalef(scale, scale, scale);
                    if (!OldLogo.invertedBlocks.get()) GL11.glRotatef(180f, 1, 0, 0);

                    blockRenderer.method_48(block, 0, .77f);
                    GL11.glPopMatrix();
                }
            }

            OldLogo.isLogoRendering = false;

            GL11.glPopMatrix();
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glViewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopAttrib();
    }
}
