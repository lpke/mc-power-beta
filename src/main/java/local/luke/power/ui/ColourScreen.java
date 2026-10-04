package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;
import local.luke.power.config.Setting;
import local.luke.power.light.LightSettings;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** RGB sliders, an exact hex field and shared saved swatches; edits stay local until Done. */
public final class ColourScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final TextInput hex;
  private int colour, dragging = -1;
  private String error = "";

  public ColourScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    this.setting = setting;
    colour = LightSettings.rgb(setting.value.getAsString());
    hex = new TextInput(format(colour), 7);
  }

  public static boolean accepts(Setting setting) {
    if (setting.kind != Setting.Kind.TEXT) return false;
    String id = setting.id.toLowerCase(Locale.ROOT);
    return (id.contains("color") || id.contains("colour"))
        && setting.value.getAsString().matches("#?[0-9a-fA-F]{6}");
  }

  private static String format(int colour) {
    return String.format(Locale.ROOT, "#%06X", colour & 0xffffff);
  }

  private int left() {
    return (width - span()) / 2;
  }

  private int span() {
    return Math.min(340, width - 24);
  }

  private int top() {
    return Math.max(28, (height - 220) / 2);
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
  }

  private void parse() {
    try {
      colour = LightSettings.rgb(hex.text());
      error = "";
    } catch (IllegalArgumentException e) {
      error = "Enter six hex digits, such as #FF5544";
    }
  }

  private void slide(int x) {
    if (dragging < 0) return;
    int channel =
        Math.max(0, Math.min(255, Math.round((x - left() - 52f) / Math.max(1, span() - 60) * 255)));
    int shift = (2 - dragging) * 8;
    colour = (colour & ~(255 << shift)) | channel << shift;
    hex.setText(format(colour));
    error = "";
  }

  protected void mouseClicked(int x, int y, int button) {
    if (button != 0 && button != 1) return;
    for (int i = 0; i < 3; i++)
      if (inside(x, y, left() + 48, top() + 22 + i * 22, span() - 48, 18)) {
        if (button == 0) {
          hex.focused = false;
          dragging = i;
          slide(x);
        }
        return;
      }
    hex.focused = inside(x, y, left(), top() + 92, span() - 108, 18);
    if (hex.focused) return;
    if (button == 0 && inside(x, y, left() + span() - 104, top() + 91, 50, 20)) {
      try {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(format(colour)), null);
        error = "";
      } catch (RuntimeException e) {
        error = "Clipboard unavailable";
      }
    }
    if (button == 0 && inside(x, y, left() + span() - 50, top() + 91, 50, 20)) {
      hex.setText(getClipboard().strip());
      parse();
    }
    if (button == 0 && inside(x, y, left(), top() + 116, 96, 20)) {
      try {
        parse();
        if (!error.isEmpty()) return;
        MenuPreferences.update(
            v -> {
              String value = format(colour);
              v.colours.removeIf(s -> s.equalsIgnoreCase(value));
              if (v.colours.size() == 16) v.colours.remove(0);
              v.colours.add(value);
            });
      } catch (Exception e) {
        error = "Could not save colour";
      }
    }
    var saved = MenuPreferences.current().colours;
    for (int i = 0; i < saved.size(); i++) {
      if (inside(x, y, left() + i % 8 * 25, top() + 140 + i / 8 * 22, 22, 20)) {
        String value = saved.get(i);
        if (button == 0) {
          colour = LightSettings.rgb(value);
          hex.setText(format(colour));
          error = "";
        } else
          try {
            MenuPreferences.update(v -> v.colours.remove(value));
          } catch (Exception e) {
            error = "Could not remove colour";
          }
        return;
      }
    }
    if (button == 0 && inside(x, y, width / 2 - 102, height - 28, 100, 20)) save();
    if (button == 0 && inside(x, y, width / 2 + 2, height - 28, 100, 20))
      minecraft.setScreen(parent);
  }

  private void save() {
    parse();
    if (!error.isEmpty()) return;
    setting.parse(format(colour));
    parent.changed(setting);
    minecraft.setScreen(parent);
  }

  protected void keyPressed(char c, int key) {
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
    if (key == Keyboard.KEY_RETURN) {
      save();
      return;
    }
    if (hex.focused) {
      hex.key(c, key);
      parse();
    }
  }

  public void render(int mx, int my, float delta) {
    if (dragging >= 0) {
      if (Mouse.isButtonDown(0)) slide(Mouse.getX() * width / minecraft.displayWidth);
      else dragging = -1;
    }
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 15, 0xffffff);
    fill(left(), top(), left() + span(), top() + 16, 0xff000000 | colour);
    String[] labels = {"Red", "Green", "Blue"};
    for (int i = 0; i < 3; i++) {
      int value = colour >> (2 - i) * 8 & 255;
      text(labels[i], left(), top() + 27 + i * 22, 0xdddddd);
      slider(
          Integer.toString(value),
          left() + 48,
          top() + 22 + i * 22,
          span() - 48,
          mx,
          my,
          value / 255d);
    }
    input(hex, left(), top() + 92, span() - 108, mx, my, "#RRGGBB");
    button("Copy", left() + span() - 104, top() + 91, 50, 20, mx, my, true);
    button("Paste", left() + span() - 50, top() + 91, 50, 20, mx, my, true);
    button("Save colour", left(), top() + 116, 96, 20, mx, my, true);
    text(fit("Right-click removes a swatch", span() - 102), left() + 102, top() + 122, 0xaaaaaa);
    var saved = MenuPreferences.current().colours;
    for (int i = 0; i < saved.size(); i++) {
      int x = left() + i % 8 * 25, y = top() + 140 + i / 8 * 22;
      fill(x, y, x + 22, y + 20, 0xff000000);
      fill(x + 1, y + 1, x + 21, y + 19, 0xff000000 | LightSettings.rgb(saved.get(i)));
      if (inside(mx, my, x, y, 22, 20)) tooltip(saved.get(i), mx, my);
    }
    if (!error.isEmpty()) text(fit(error, span()), left(), height - 43, 0xff8888);
    button("Done", width / 2 - 102, height - 28, 100, 20, mx, my, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, mx, my, true);
  }
}
