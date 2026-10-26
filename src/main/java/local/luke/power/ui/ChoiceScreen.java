package local.luke.power.ui;

import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/** A shared choice popup over a native screen that does not inherit our controls. */
public final class ChoiceScreen extends UiScreen {
  private final Screen parent;
  private final List<String> labels, help;
  private final int selected, defaultIndex, left, top, span;
  private final IntConsumer accepted;
  private final SettingsDropdown dropdown = new SettingsDropdown();

  public ChoiceScreen(Screen parent, List<String> labels, List<String> help, int selected,
      int defaultIndex, int left, int top, int span, IntConsumer accepted) {
    this.parent = parent; this.labels = List.copyOf(labels); this.help = List.copyOf(help);
    this.selected = selected; this.defaultIndex = defaultIndex;
    this.left = left; this.top = top; this.span = span; this.accepted = accepted;
  }
  public void init() {
    dropdown.open(this, labels, help, selected, defaultIndex, left, top, span, 20, width, height, index -> {
      accepted.accept(index); minecraft.setScreen(parent);
    });
  }
  public void tick() { dropdown.tick(); }
  public void removed() { dropdown.close(); }
  private void returnIfClosed() { if (!dropdown.active() && minecraft.currentScreen == this) minecraft.setScreen(parent); }
  protected void mouseClicked(int x, int y, int button) { dropdown.press(this, x, y, button); returnIfClosed(); }
  protected void mouseReleased(int x, int y, int button) { dropdown.release(y, button); }
  protected void keyPressed(char c, int key) { dropdown.key(this, key); returnIfClosed(); }
  public void onMouseEvent() { super.onMouseEvent(); dropdown.wheel(Mouse.getEventDWheel()); }
  public void render(int x, int y, float delta) {
    parent.render(-1000, -1000, delta); dropdown.render(this, x, y); returnIfClosed();
  }
  public boolean shouldPause() { return parent.shouldPause(); }
}
