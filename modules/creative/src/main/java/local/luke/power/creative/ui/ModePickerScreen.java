package local.luke.power.creative.ui;

import local.luke.power.creative.*;
import local.luke.power.creative.api.*;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderHelper;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class ModePickerScreen extends Screen {
  private static final GameMode[] MODES = {
    GameMode.CREATIVE, GameMode.SURVIVAL, GameMode.SPECTATOR
  };
  private final ItemRenderer items = new ItemRenderer();
  private GameMode selected;
  private int lastX, lastY;
  private boolean hovered, finished, cycleReady;

  @Override
  public void init() {
    if (!ClientRuntime.local(minecraft)) {
      minecraft.openScreen(null);
      return;
    }
    if (selected == null) selected = ((ModePlayer) minecraft.player).power_previousMode();
    if (!Modes.denial(selected).isEmpty()) selected = GameMode.SURVIVAL;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void tick() {
    finishIfReleased();
  }

  private boolean finishIfReleased() {
    if (!local.luke.power.input.Bindings.down(Keys.PICKER)) cycleReady = true;
    if (finished) return true;
    if (!ClientRuntime.local(minecraft) || !Display.isActive() || ClientRuntime.freecam()) {
      finished = true;
      minecraft.openScreen(null);
      return true;
    }
    if (!local.luke.power.input.Bindings.down(Keys.MODIFIER)) {
      finished = true;
      Modes.change(minecraft, selected);
      minecraft.openScreen(null);
      return true;
    }
    return false;
  }

  @Override
  protected void keyPressed(char c, int code) {
    if (code == Keyboard.KEY_ESCAPE) {
      finished = true;
      minecraft.openScreen(null);
    } else if (code == Keys.PICKER.key && cycleReady && !Keyboard.isRepeatEvent()) {
      int i = java.util.Arrays.asList(MODES).indexOf(selected);
      for (int n = 1; n <= MODES.length; n++) {
        GameMode candidate = MODES[(i + n) % MODES.length];
        if (Modes.denial(candidate).isEmpty()) { selected = candidate; break; }
      }
      hovered = false;
      cycleReady = false;
    }
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    if (button == 0) choose(x, y);
    else if (button - 100 == Keys.PICKER.key) keyPressed(' ', Keys.PICKER.key);
  }

  private void choose(int x, int y) {
    int left = width / 2 - 44, top = height / 2 - 31;
    if (y < top || y >= top + 26) return;
    for (int i = 0; i < 3; i++)
      if (x >= left + i * 31 && x < left + i * 31 + 26 && Modes.denial(MODES[i]).isEmpty()) selected = MODES[i];
  }

  @Override
  public void render(int x, int y, float delta) {
    if (finishIfReleased()) return;
    if (!hovered) {
      lastX = x;
      lastY = y;
      hovered = true;
    }
    if (x != lastX || y != lastY) choose(x, y);
    int left = width / 2 - 44, top = height / 2 - 31;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      Texture.draw(minecraft, "picker", width / 2 - 62, top - 27, 125, 59, 0, 0, 125, 75, 128, 128);
      // Centre the eight-pixel text and shadow within the vertically scaled header.
      drawTextWithShadowCentred(textManager, selected.label, width / 2, top - 22, 0xFFFFFF);
      for (int i = 0; i < 3; i++) {
        int sx = left + i * 31;
        Texture.draw(minecraft, "mode_slot", sx, top, 26, 26, 0, 0, 26, 26, 26, 26);
        if (MODES[i] == GameMode.SPECTATOR)
          Texture.draw(minecraft, "ender_eye", sx + 5, top + 5, 16, 16, 0, 0, 16, 16, 16, 16);
        else {
          GL11.glPushMatrix();
          GL11.glRotatef(120, 1, 0, 0);
          RenderHelper.enableLighting();
          GL11.glEnable(GL12.GL_RESCALE_NORMAL);
          GL11.glPopMatrix();
          ItemStack icon =
              MODES[i] == GameMode.CREATIVE ? new ItemStack(Block.GRASS) : new ItemStack(267, 1, 0);
          items.renderStackInGUI(textManager, minecraft.textureManager, icon, sx + 5, top + 5);
          RenderHelper.disableLighting();
        }
        if (!Modes.denial(MODES[i]).isEmpty()) {
          fill(sx + 1, top + 1, sx + 25, top + 25, 0x99000000);
          drawTextWithShadowCentred(textManager, "x", sx + 13, top + 9, 0xbb7777);
        }
        if (selected == MODES[i])
          Texture.draw(minecraft, "mode_selection", sx, top, 26, 26, 0, 0, 26, 26, 26, 26);
      }
    } finally {
      GL11.glPopAttrib();
    }
  }
}
