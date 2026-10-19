package local.luke.power.sprint;

import local.luke.power.ui.StatusHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.class_564;
import org.lwjgl.opengl.GL11;

/** Survival stamina follows the hotbar and shares native nine-pixel icon spacing. */
public final class SprintStaminaHud {
  private static final String[] BOLT = {
    "000011100", "000111000", "001110000", "011111110", "000111100",
    "000111000", "001110000", "001100000", "011000000"};
  private SprintStaminaHud() {}
  public static void render(Minecraft mc) {
    var settings = SprintConfig.current();
    if (!settings.stamina || !SprintController.allowed(mc) || mc.player == null || mc.options.hideHud
        || mc.player.health <= 0 || mc.player.dead || !SprintController.usesStamina(mc.player)
        || mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
    var state = ((SprintPlayer)mc.player).power$sprint();
    if (state.stamina >= settings.capacity) return;
    var screen = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int right = screen.method_1857() / 2 + 91, y = screen.method_1858() - 32;
    if (mc.inGameHud instanceof StatusHudLayout layout) {
      if (!layout.power$hotbarVisible()) return;
      right = layout.power$statusRight(); y = layout.power$statusY();
      y = StaminaIcons.y(right, y, layout.power$armorVisible() && mc.player.inventory.method_687() > 0,
          layout.power$armorX(), layout.power$armorY(), screen.method_1858());
    } else if (mc.player.inventory.method_687() > 0) y -= 10;
    int units = StaminaIcons.units(state.stamina, settings.capacity);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glBegin(GL11.GL_QUADS);
      for (int i = 0; i < 10; i++) {
        int x = right - 81 + i * 8;
        for (int py = 0; py < 9; py++) for (int px = 0; px < 9; px++) {
          boolean bolt = bit(px, py);
          boolean border = !bolt && (bit(px - 1, py) || bit(px + 1, py) || bit(px, py - 1) || bit(px, py + 1));
          if (!bolt && !border) continue;
          boolean filled = units >= (i + 1) * 2 || units == i * 2 + 1 && px < 5;
          int color = border ? 0x30220c : !filled ? 0x61543a : py < 4 ? 0xffe58a : 0xe2b32f;
          GL11.glColor3ub((byte)(color >> 16), (byte)(color >> 8), (byte)color);
          GL11.glVertex2i(x + px, y + py); GL11.glVertex2i(x + px + 1, y + py);
          GL11.glVertex2i(x + px + 1, y + py + 1); GL11.glVertex2i(x + px, y + py + 1);
        }
      }
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
  }
  private static boolean bit(int x, int y) { return x >= 0 && x < 9 && y >= 0 && y < 9 && BOLT[y].charAt(x) == '1'; }
}
