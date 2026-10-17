package local.luke.power.ui;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Shared Save/Discard/Cancel dialog for editors whose draft has not reached the parent. */
final class DraftExitPrompt {
  private boolean active;
  boolean active() { return active; }

  void leave(boolean changed, Runnable discard) {
    TextDrag.cancel();
    if (changed) active = true;
    else discard.run();
  }

  boolean key(int key) {
    if (!active) return false;
    if (key == Keyboard.KEY_ESCAPE) active = false;
    return true;
  }

  private int buttonWidth(UiScreen screen) { return Math.min(76, Math.max(10, (screen.width - 32) / 3)); }
  private int buttonX(UiScreen screen, int index) {
    int width = buttonWidth(screen);
    return (screen.width - (width * 3 + 8)) / 2 + index * (width + 4);
  }

  boolean click(UiScreen screen, int x, int y, int button, Runnable save, Runnable discard) {
    if (!active) return false;
    if (button == 0) for (int i = 0; i < 3; i++)
      if (screen.inside(x, y, buttonX(screen, i), screen.height / 2 + 8, buttonWidth(screen), 20)) {
        screen.click();
        active = false;
        if (i == 0) save.run();
        else if (i == 1) discard.run();
        break;
      }
    return true;
  }

  void render(UiScreen screen, String title, int x, int y) {
    if (!active) return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      screen.rectangle(0, 0, screen.width, screen.height, 0xc0000000);
      screen.centeredText(screen.fit(title, screen.width - 24), screen.height / 2 - 16, 0xffffff);
      String[] labels = {"Save", "Discard", "Cancel"};
      for (int i = 0; i < labels.length; i++)
        screen.button(labels[i], buttonX(screen, i), screen.height / 2 + 8, buttonWidth(screen), 20, x, y, true);
    } finally { GL11.glPopAttrib(); }
  }
}
