package local.luke.power.ui;

import java.util.Objects;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;

/** An isolated value draft that occupies exactly the control it replaces. */
final class InlineValueEditor {
  private Object key;
  private Setting setting;
  private Runnable accepted;
  private TextInput input;
  private String error = "";
  private boolean rendered;
  private UiScreen screen;
  private long errorUntil;
  private int x, y, width, clipTop, clipBottom;
  private InlineValueGeometry geometry() { return new InlineValueGeometry(x, y, width); }

  boolean active() { return setting != null; }
  boolean editing(Object candidate) { return active() && Objects.equals(key, candidate); }
  void beginFrame() { rendered = false; }
  void endFrame() { if (active() && !rendered) cancel(); }

  void begin(Object key, Setting setting, Runnable accepted,
      int x, int y, int width, int clipTop, int clipBottom) {
    this.key = key;
    this.setting = setting;
    this.accepted = accepted;
    input = new TextInput(setting.editText(), setting.kind == Setting.Kind.TEXT ? 4096 : setting.kind == Setting.Kind.LIST ? 16384 : 64);
    input.focused = true;
    input.selectAll();
    error = "";
    bounds(x, y, width, clipTop, clipBottom);
  }

  private void bounds(int x, int y, int width, int clipTop, int clipBottom) {
    this.x = x; this.y = y; this.width = width;
    this.clipTop = clipTop; this.clipBottom = clipBottom;
  }

  void cancel() { if (input != null) input.focused = false; key = null; setting = null; accepted = null; input = null; error = ""; }

  boolean press(int mx, int my, int button) {
    if (!active()) return false;
    if (!geometry().contains(mx, my, clipTop, clipBottom)) {
      cancel();
      return false;
    }
    if (button == 0 && geometry().checkContains(mx, my, clipTop, clipBottom)) commit();
    else if (button == 1 && !TextEditing.rightClickClearEnabled()) input.selectAll();
    else input.click(mx, button);
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
      error = "Enter a number";
      errorUntil = System.nanoTime() + 4_000_000_000L;
      return;
    } catch (IllegalArgumentException e) {
      error = Objects.toString(e.getMessage(), "Invalid value");
      errorUntil = System.nanoTime() + 4_000_000_000L;
      return;
    }
    Runnable callback = accepted;
    if (screen != null) screen.click();
    cancel(); // Applying GUI scale can reinitialize this screen immediately.
    callback.run();
  }

  void render(UiScreen screen, int x, int y, int width, int mx, int my, int clipTop, int clipBottom) {
    this.screen = screen;
    if (System.nanoTime() >= errorUntil) error = "";
    bounds(x, y, width, clipTop, clipBottom);
    rendered = y + 18 > clipTop && y < clipBottom;
    InlineValueGeometry bounds = geometry();
    screen.input(input, x + 1, y + 1, bounds.inputWidth(), InlineValueGeometry.HEIGHT - 2, mx, my, "");
    screen.iconButton("check", bounds.checkX(), y, bounds.checkWidth(), mx, my, true);
    if (!error.isEmpty()) screen.rectangle(x, y + 17, bounds.checkX() - InlineValueGeometry.GAP, y + 18, 0xffff7777);
  }

  void renderError(UiScreen screen) {
    if (active() && !error.isEmpty() && System.nanoTime() < errorUntil) screen.tooltip(error, x, y + 18);
  }

  String help() { return error; }
}
