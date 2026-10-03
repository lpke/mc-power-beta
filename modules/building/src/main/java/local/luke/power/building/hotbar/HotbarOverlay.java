package local.luke.power.building.hotbar;

import local.luke.power.building.config.Config;
import net.minecraft.class_564;
import net.minecraft.class_583;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import org.lwjgl.opengl.GL11;

/** Tweakeroo's three-row preview and release-to-swap selector, rendered with Beta items. */
public final class HotbarOverlay extends DrawableHelper {
  private static final HotbarOverlay INSTANCE = new HotbarOverlay();
  private final ItemRenderer items = new ItemRenderer();

  public static void render(Minecraft mc) {
    boolean swap = Hotbars.showSwap(mc), scroll = !swap && Hotbars.showScroll(mc);
    if (!swap && !scroll) return;
    INSTANCE.draw(mc, scroll);
  }

  private void draw(Minecraft mc, boolean scroll) {
    class_564 size = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int w = size.method_1857(), h = size.method_1858();
    HotbarSettings s = Config.current().hotbar;
    int x = s.offsetX, y = s.offsetY;
    if (scroll) {
      x = w / 2 - 81;
      y = h / 2 + 14;
    } else
      switch (s.alignment) {
        case TOP_RIGHT -> x = w - 162 - s.offsetX;
        case BOTTOM_LEFT -> y = h - 54 - s.offsetY;
        case BOTTOM_RIGHT -> {
          x = w - 162 - s.offsetX;
          y = h - 54 - s.offsetY;
        }
        case CENTER -> {
          x = w / 2 - 81 - s.offsetX;
          y = h / 2 - 27 - s.offsetY;
        }
        default -> {}
      }
    x = Math.max(12, Math.min(w - 166, x));
    y = Math.max(4, Math.min(h - (scroll ? 96 : 58), y));
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glColor4f(1, 1, 1, 1);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId("/gui/inventory.png"));
      drawTexture(x - 1, y - 1, 7, 83, 162, 54);
      if (scroll) drawTexture(x - 1, y + 69, 7, 141, 162, 18);
      if (s.numberRowKeys)
        for (int row = 0; row < 3; row++)
          mc.textRenderer.drawWithShadow(
              Integer.toString(row + 1), x - 10, y + row * 18 + 4, 0xFFFFFF);
      if (scroll) {
        int sy = y + Hotbars.row() * 18;
        fill(x - 2, sy - 2, x + 162, sy, 0xFFFF4040);
        fill(x - 2, sy + 17, x + 162, sy + 19, 0xFFFF4040);
        fill(x - 2, sy, x, sy + 17, 0xFFFF4040);
        fill(x + 160, sy, x + 162, sy + 17, 0xFFFF4040);
      }
      GL11.glPushMatrix();
      GL11.glRotatef(120, 1, 0, 0);
      class_583.method_1930();
      GL11.glPopMatrix();
      GL11.glEnable(32826);
      for (int i = 9; i < 36; i++) item(mc, i, x + (i % 9) * 18, y + ((i - 9) / 9) * 18);
      if (scroll) for (int i = 0; i < 9; i++) item(mc, i, x + i * 18, y + 70);
      class_583.method_1927();
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private void item(Minecraft mc, int slot, int x, int y) {
    var stack = mc.player.inventory.main[slot];
    if (stack == null) return;
    items.method_1487(mc.textRenderer, mc.textureManager, stack, x, y);
    items.method_1488(mc.textRenderer, mc.textureManager, stack, x, y);
  }
}
