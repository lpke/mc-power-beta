package local.luke.power.title;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Original Mojang title textures, drawn without altering retro lettering preferences. */
public final class TitleLogos {
    private static final String ROOT = "/assets/power_title/logos/";

    private TitleLogos() {}

    public static boolean textured() {
        int style = TitleFeatures.style.get();
        return style > 0 && style < TitleFeatures.TITLE_STYLES.size();
    }

    public static void render(Minecraft minecraft, int width) {
        int style = TitleFeatures.style.get();
        String file = switch (style) {
            case 1 -> "beta.png";
            case 2 -> "java_2017.png";
            case 3 -> "java_2020.png";
            default -> "modern.png";
        };
        double scale = Math.min(1, Math.max(1, width - 20) / 310.0);
        double left = (width - 310 * scale) / 2;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId(ROOT + file));
            if (style == 4) {
                quad(left, 20, 310 * scale, 77.5 * scale, 0, 0, 1, 1);
            } else {
                quad(left, 30, 155 * scale, 44 * scale, 0, 0, 155.0 / 256, 44.0 / 256);
                quad(left + 155 * scale, 30, 155 * scale, 44 * scale, 0, 45.0 / 256, 155.0 / 256, 89.0 / 256);
            }
            if (style > 1) {
                String edition = style == 4 ? "edition_modern.png" : "edition.png";
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId(ROOT + edition));
                quad((width - 128 * scale) / 2, style == 4 ? 20 + 53 * scale : 30 + 37 * scale,
                        128 * scale, 16 * scale, 0, 0, 1, 1);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void quad(double x, double y, double width, double height, double u, double v, double right, double bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(u, bottom); GL11.glVertex3d(x, y + height, 0);
        GL11.glTexCoord2d(right, bottom); GL11.glVertex3d(x + width, y + height, 0);
        GL11.glTexCoord2d(right, v); GL11.glVertex3d(x + width, y, 0);
        GL11.glTexCoord2d(u, v); GL11.glVertex3d(x, y, 0);
        GL11.glEnd();
    }
}
