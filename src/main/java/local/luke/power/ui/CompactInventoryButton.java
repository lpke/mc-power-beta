package local.luke.power.ui;

import local.luke.power.input.GuiSprite;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Asset-backed ten-pixel inventory buttons and six-pixel letter sprites. */
public final class CompactInventoryButton {
  private static final String ROOT = "/assets/powerbeta/textures/gui/";
  private static final String LABELS = "SDHR";
  private CompactInventoryButton() {}

  public static void draw(Minecraft mc, String label, int x, int y, boolean enabled) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glColor4f(1, 1, 1, 1);
      mc.textureManager.bindTexture(mc.textureManager.getTextureId(ROOT + "compact_button.png"));
      GuiSprite.draw(x, y, 10, 10, 0, 0, 10, 10, 10, 10);
      int index = LABELS.indexOf(label);
      if (index >= 0 && label.length() == 1) {
        mc.textureManager.bindTexture(mc.textureManager.getTextureId(ROOT + "compact_letters.png"));
        GuiSprite.draw(x + 2, y + 2, 6, 6, index * 6, enabled ? 0 : 6, 6, 6, 24, 12);
      }
    } finally { GL11.glPopAttrib(); }
  }
}
