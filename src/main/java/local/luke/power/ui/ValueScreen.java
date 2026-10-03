package local.luke.power.ui;

import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;

public final class ValueScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final TextInput input;
  private String error = "";

  public ValueScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    input = new TextInput(setting.editText(), setting.kind == Setting.Kind.LIST ? 16384 : 4096);
    input.focused = true;
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
  }

  private int left() {
    return Math.max(12, (width - 480) / 2);
  }

  private int span() {
    return width - 2 * left();
  }

  protected void mouseClicked(int x, int y, int b) {
    if (b != 0 && b != 1) return;
    if (inside(x, y, left(), height / 2 - 9, span(), 18)) {
      input.focused = true;
      if (b == 1) {
        input.selectAll();
        input.key((char) 0, Keyboard.KEY_BACK);
      }
      return;
    }
    if (b != 0) return;
    if (inside(x, y, width / 2 - 102, height - 28, 100, 20)) save();
    if (inside(x, y, width / 2 + 2, height - 28, 100, 20)) minecraft.setScreen(parent);
  }

  private void save() {
    try {
      setting.parse(input.text);
      minecraft.setScreen(parent);
    } catch (RuntimeException e) {
      error = e.getMessage() == null ? "Invalid value" : e.getMessage();
    }
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
      save();
      return;
    }
    input.key(c, key);
    error = "";
  }

  public void render(int x, int y, float delta) {
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 18, 0xffffff);
    text(fit(setting.description, span()), left(), 40, 0xaaaaaa);
    input(input, left(), height / 2 - 9, span(), x, y, "");
    text(fit(error, span()), left(), height / 2 + 18, 0xff7777);
    String hint =
        setting.kind == Setting.Kind.LIST
            ? "Use a JSON list. Ctrl+A selects all; Ctrl+V pastes."
            : setting.kind == Setting.Kind.TEXT
                ? "Ctrl+A selects all; Ctrl+V pastes."
                : "Range: " + Setting.number(setting.min) + " to " + Setting.number(setting.max);
    text(fit(hint, span()), left(), height / 2 + 36, 0xaaaaaa);
    button("Done", width / 2 - 102, height - 28, 100, 20, x, y, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, x, y, true);
  }
}
