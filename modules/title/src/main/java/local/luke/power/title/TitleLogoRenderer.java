package local.luke.power.title;

import java.util.*;
import local.luke.power.ui.TitlePreview;
import net.minecraft.block.Block;
import net.minecraft.class_13;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** The title and its draft preview use the same bounded native block renderer. */
public final class TitleLogoRenderer implements TitlePreview.Renderer {
    private final Random random = new Random();
    private final class_13 blockRenderer = new class_13();
    private LogoEffectRandomizer[][] logoEffects;
    public static boolean supportsBlock(int id) {
        if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return false;
        return switch (Block.BLOCKS[id].method_1621()) {
            case 0, 1, 2, 6, 10, 11, 13, 16, 18 -> true;
            default -> false;
        };
    }
    public void replay() { logoEffects = null; }
    public boolean settled() {
        if (logoEffects == null) return false;
        for (var column : logoEffects) for (var effect : column) if (effect.target > 0) return false;
        return true;
    }
    public void tick() {
        if (logoEffects != null) for (var column : logoEffects) for (var effect : column) effect.tick();
    }
    public void draw(int x, int y, int width, int height, int screenWidth, int screenHeight,
            String[] logo, Map<Character, Integer> blocks, float[] rotation,
            boolean inverted, boolean shadow, boolean animation, float speed, float delta) {
        Minecraft minecraft = TitleFeatures.minecraft;
        if (minecraft == null || width <= 0 || height <= 0 || logo == null || logo.length == 0) return;
        int columns = Arrays.stream(logo).mapToInt(String::length).max().orElse(0);
        if (columns == 0 || columns > 80 || logo.length > 12) return;
        if (logoEffects == null || logoEffects.length != columns || logoEffects[0].length != logo.length) {
            logoEffects = new LogoEffectRandomizer[columns][logo.length];
            for (int col = 0; col < columns; col++) for (int row = 0; row < logo.length; row++)
                logoEffects[col][row] = new LogoEffectRandomizer(random, col, row, speed);
        }
        for (var column : logoEffects) for (var effect : column) effect.speed = speed;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_VIEWPORT_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_CURRENT_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        int vx = x * minecraft.displayWidth / screenWidth;
        int vy = minecraft.displayHeight - (y + height) * minecraft.displayHeight / screenHeight;
        int vw = Math.max(1, width * minecraft.displayWidth / screenWidth);
        int viewHeight = Math.max(1, height * minecraft.displayHeight / screenHeight);

        GLU.gluPerspective(70, (float) vw / (float) viewHeight, .05f, 100);
        GL11.glViewport(vx, vy, vw, viewHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(vx, vy, vw, viewHeight);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        try {

            for (int pass = 0; pass < 3; pass++) {
                if (pass == 0 && !shadow) continue;

                GL11.glPushMatrix();
                try {
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

                    float[] rot = rotation;
                    GL11.glRotatef(rot[0], 1f, 0f, 0f); // X
                    GL11.glRotatef(rot[1], 0f, 1f, 0f); // Y
                    GL11.glRotatef(rot[2], 0f, 0f, 1f); // Z

                    GL11.glScalef(.89f, 1, .4f);
                    GL11.glTranslatef(-columns * .5f, -logo.length * .5f, 0);

                    if (pass == 0) GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/title/black.png"));
                    else GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/terrain.png"));

                    TitleFeatures.isLogoRendering = true;

                    for (int row = 0; row < logo.length; row++) {
                        for (int column = 0; column < logo[row].length(); column++) {
                            Integer id = blocks.get(logo[row].charAt(column));
                            if (id == null || id == 0) continue;

                            Block block = id > 0 && id < Block.BLOCKS.length ? Block.BLOCKS[id] : null;
                            if (block == null) continue;

                            GL11.glPushMatrix();
                            try {
                                LogoEffectRandomizer effect = logoEffects[column][row];

                                float z = 0.0f;
                                float scale = 1;

                                if (animation) z = (float) (effect.current + (effect.target - effect.current) * delta);
                                if (pass == 0) {
                                    scale = z * .04f + 1;
                                    z = 0;
                                }

                                GL11.glTranslatef(column, row, z);
                                GL11.glScalef(scale, scale, scale);
                                if (!inverted) GL11.glRotatef(180f, 1, 0, 0);

                                blockRenderer.method_48(block, 0, .77f);
                            } finally { GL11.glPopMatrix(); }
                        }
                    }

                    TitleFeatures.isLogoRendering = false;

                } finally { GL11.glPopMatrix(); }
            }

        } finally {
            TitleFeatures.isLogoRendering = false;
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
}
