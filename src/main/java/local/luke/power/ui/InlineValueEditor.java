package local.luke.power.ui;

import java.util.Objects;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;

/** An isolated numeric draft that occupies exactly the slider it replaces. */
final class InlineValueEditor {
  private Object key;
  private Setting setting;
  private Runnable accepted;
  private TextInput input;
  private String error = "";
  private boolean rendered;
  private int x, y, width, clipTop, clipBottom;
  private static final int CHECK_WIDTH = 18, GAP = 2;

  boolean active() { return setting != null; }
  boolean editing(Object candidate) { return active() && Objects.equals(key, candidate); }
  void beginFrame() { rendered = false; }
  void endFrame() { if (active() && !rendered) cancel(); }

  void begin(Object key, Setting setting, Runnable accepted,
      int x, int y, int width, int clipTop, int clipBottom) {
    this.key = key;
    this.setting = setting;
    this.accepted = accepted;
    input = new TextInput(setting.editText(), 64);
    input.focused = true;
    input.selectAll();
    error = "";
    bounds(x, y, width, clipTop, clipBottom);
  }

  private void bounds(int x, int y, int width, int clipTop, int clipBottom) {
    this.x = x; this.y = y; this.width = width;
    this.clipTop = clipTop; this.clipBottom = clipBottom;
  }

  void cancel() { key = null; setting = null; accepted = null; input = null; error = ""; }

  boolean press(int mx, int my, int button) {
    if (!active()) return false;
    if (mx < x || mx >= x + width || my < Math.max(y, clipTop) || my >= Math.min(y + 18, clipBottom)) {
      cancel();
      return false;
    }
    if (button == 0 && mx >= x + width - CHECK_WIDTH) commit();
    else if (button == 1) input.selectAll();
    return true;
  }

  boolean key(char c, int code) {
    if (!active()) return false;
    if (code == Keyboard.KEY_ESCAPE) cancel();
    else if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) commit();
    else { input.key(c, code); error = ""; }
    return true;
  }

  private void commit() {
    try {
      setting.parse(input.text());
    } catch (NumberFormatException | ArithmeticException e) {
      error = setting.kind == Setting.Kind.INTEGER ? "Enter a whole number" : "Enter a number";
      return;
    } catch (IllegalArgumentException e) {
      error = Objects.toString(e.getMessage(), "Invalid value");
      return;
    }
    Runnable callback = accepted;
    cancel(); // Applying GUI scale can reinitialize this screen immediately.
    callback.run();
  }

  void render(UiScreen screen, int x, int y, int width, int mx, int my, int clipTop, int clipBottom) {
    bounds(x, y, width, clipTop, clipBottom);
    rendered = y + 18 > clipTop && y < clipBottom;
    // UiScreen.input has a one-pixel border. Keep it inside the original slider bounds.
    screen.input(input, x + 1, y, Math.max(8, width - CHECK_WIDTH - GAP - 2), mx, my, "");
    screen.iconButton("check", x + width - CHECK_WIDTH, y, CHECK_WIDTH, mx, my, true);
    if (!error.isEmpty()) screen.rectangle(x, y + 17, x + width - CHECK_WIDTH - GAP, y + 18, 0xffff7777);
  }

  String help() { return error.isEmpty() ? "Enter or check to apply. Escape or click elsewhere to cancel." : error; }
}
