package local.luke.power.ui;

import java.util.function.BiConsumer;
import local.luke.power.map.MapMath;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/** Navigates archived map data without moving the player or requesting native chunks. */
public final class MapCoordinatesScreen extends UiScreen {
  private final Screen parent;
  private final TextInput x, z;
  private final BiConsumer<Double, Double> accept;
  private String error = "";

  public MapCoordinatesScreen(
      Screen parent, double x, double z, BiConsumer<Double, Double> accept) {
    this.parent = parent;
    this.accept = accept;
    this.x = new TextInput(Long.toString((long) Math.floor(x)), 10);
    this.z = new TextInput(Long.toString((long) Math.floor(z)), 10);
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
    x.focused = true;
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
    TextDrag.cancel();
  }

  private int left() {
    return (width - span()) / 2;
  }

  private int span() {
    return Math.min(260, width - 20);
  }

  private void done() {
    try {
      int px = Integer.parseInt(x.text().trim()), pz = Integer.parseInt(z.text().trim());
      if (Math.abs((long) px) > MapMath.BORDER || Math.abs((long) pz) > MapMath.BORDER)
        throw new NumberFormatException();
      accept.accept((double) px, (double) pz);
      minecraft.setScreen(parent);
    } catch (NumberFormatException e) {
      error = "Enter whole coordinates within 32,000,000 blocks";
    }
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
      click();
      done();
      return;
    }
    if (key == Keyboard.KEY_TAB) {
      x.focused = !x.focused;
      z.focused = !x.focused;
      return;
    }
    x.key(c, key);
    z.key(c, key);
  }

  protected void mouseClicked(int mx, int my, int button) {
    if (button != 0) return;
    x.focused = inside(mx, my, left() + 24, 42, span() - 24, 18);
    x.click(mx, button);
    z.focused = inside(mx, my, left() + 24, 68, span() - 24, 18);
    z.click(mx, button);
    if (inside(mx, my, left(), 100, span() / 2 - 2, 20)) {
      click();
      done();
    } else if (inside(mx, my, left() + span() / 2 + 2, 100, span() / 2 - 2, 20)) {
      click();
      minecraft.setScreen(parent);
    }
  }

  public void render(int mx, int my, float delta) {
    renderBackground();
    titleBar("Go to map coordinates");
    text("X", left(), 47, 0xcccccc);
    input(x, left() + 24, 42, span() - 24, mx, my, "");
    text("Z", left(), 73, 0xcccccc);
    input(z, left() + 24, 68, span() - 24, mx, my, "");
    button("Go to", left(), 100, span() / 2 - 2, 20, mx, my, true);
    button("Cancel", left() + span() / 2 + 2, 100, span() / 2 - 2, 20, mx, my, true);
    if (!error.isEmpty()) text(fit(error, width - 20), 10, 132, 0xff7777);
  }

  public boolean shouldPause() {
    return local.luke.power.map.MapConfig.current().pauseMap;
  }
}
