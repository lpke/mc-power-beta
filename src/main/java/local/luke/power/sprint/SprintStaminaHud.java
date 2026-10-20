package local.luke.power.sprint;

import local.luke.power.ui.StatusHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.class_525;
import net.minecraft.class_564;
import org.lwjgl.opengl.GL11;

/** Survival stamina shares native status geometry with the isolated HUD editor. */
public final class SprintStaminaHud {
  private SprintStaminaHud() {}
  public static void render(Minecraft mc) {
    var settings = SprintConfig.current();
    if (!settings.stamina || !SprintController.allowed(mc) || mc.player == null || mc.options.hideHud
        || mc.player.health <= 0 || mc.player.dead || !SprintController.usesStamina(mc.player)
        || mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof class_525)) return;
    var state = ((SprintPlayer)mc.player).power$sprint();
    if (state.stamina >= settings.capacity) return;
    var screen = new class_564(mc.options, mc.displayWidth, mc.displayHeight);
    int right = screen.method_1857() / 2 + 91, y = screen.method_1858() - 32;
    if (mc.inGameHud instanceof StatusHudLayout layout) {
      if (!layout.power$staminaVisible()) return;
      right = layout.power$staminaX() + 81; y = layout.power$staminaY();
      y = local.luke.power.ui.NativeHudLayout.staminaY(right - 81, y, layout.power$armorVisible() && mc.player.inventory.method_687() > 0,
          layout.power$armorX(), layout.power$armorY(), screen.method_1858(),
          layout.power$staminaOffsetX(), layout.power$staminaOffsetY());
    } else if (mc.player.inventory.method_687() > 0) y -= 10;
    int units = StaminaIcons.units(state.stamina, settings.capacity);
    draw(right - 81, y, units, settings.restartStamina);
  }
  /** Shared pixel geometry and palette for gameplay and the isolated HUD preview. */
  public static void draw(int left, int y, int units, double restartPercent) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glBegin(GL11.GL_QUADS);
      for (int i = 0; i < 10; i++) {
        int x = StaminaIcons.x(left, i);
        for (int py = 0; py < 9; py++) for (int px = 0; px < 9; px++) {
          int color = StaminaIcons.color(i, units, restartPercent, px, py);
          if (color < 0) continue;
          GL11.glColor3ub((byte)(color >> 16), (byte)(color >> 8), (byte)color);
          GL11.glVertex2i(x + px, y + py); GL11.glVertex2i(x + px + 1, y + py);
          GL11.glVertex2i(x + px + 1, y + py + 1); GL11.glVertex2i(x + px, y + py + 1);
        }
      }
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
  }
}
