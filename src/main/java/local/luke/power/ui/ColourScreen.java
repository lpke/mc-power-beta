package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import local.luke.power.config.Catalog;
import local.luke.power.config.ColorSettings;
import local.luke.power.config.Setting;
import local.luke.power.light.LightSettings;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** RGB, opacity and saved swatches remain private until Done accepts the whole edit. */
public final class ColourScreen extends UiScreen {
  private final net.minecraft.client.gui.screen.Screen parent;
  private final Runnable accepted;
  private final Setting setting, opacity, draftColors;
  private final TextInput hex;
  private final InlineValueEditor inline = new InlineValueEditor();
  private final List<String> colors;
  private final List<String> originalColors;
  private final Setting localOpacity;
  private int color, dragging = -1;
  private String error = "";

  public ColourScreen(PowerOptionsScreen parent, Setting setting) {
    this(parent, setting, ColorSettings.opacity(setting, parent.session().settings()), () -> parent.changed(setting),
        parent.session().settings().stream().filter(s -> s.id.equals("interface.colours")).findFirst().orElse(null));
  }

  public ColourScreen(net.minecraft.client.gui.screen.Screen parent, Setting setting, Runnable accepted) {
    this(parent, setting, null, accepted, null);
  }

  ColourScreen(net.minecraft.client.gui.screen.Screen parent, Setting setting, Runnable accepted, Setting colors) {
    this(parent, setting, null, accepted, colors);
  }

  public ColourScreen(net.minecraft.client.gui.screen.Screen parent, Setting setting, Setting opacity, Runnable accepted) {
    this(parent, setting, opacity, accepted, null);
  }

  public ColourScreen(net.minecraft.client.gui.screen.Screen parent, Setting setting, Setting opacity, Runnable accepted, Setting colors) {
    this.parent = parent;
    this.accepted = accepted;
    this.setting = setting;
    this.opacity = opacity;
    this.draftColors = colors;
    color = ColorSettings.rgb(setting);
    hex = new TextInput(ColorSettings.hex(color), 7);
    localOpacity = opacity == null ? null : new Setting(opacity.id, opacity.backend, opacity.page, opacity.group,
        opacity.label, opacity.description, opacity.kind, opacity.value, opacity.powerBetaDefault,
        opacity.min, opacity.max, opacity.step, opacity.choices, opacity.restart);
    this.colors = new ArrayList<>();
    if (colors == null) this.colors.addAll(MenuPreferences.current().colours);
    else colors.value.getAsJsonArray().forEach(value -> this.colors.add(value.getAsString()));
    originalColors = List.copyOf(this.colors);
  }

  public net.minecraft.client.gui.screen.Screen parentScreen() { return parent; }

  public static boolean accepts(Setting setting) { return ColorSettings.accepts(setting); }
  private int left() { return (width - span()) / 2; }
  private int span() { return Math.min(340, width - 24); }
  private int top() { return Math.max(28, (height - (opacity == null ? 204 : 222)) / 2); }
  private int alphaHeight() { return opacity == null ? 0 : 18; }
  private int hexY() { return top() + 74 + alphaHeight(); }
  private int saveY() { return hexY() + 22; }
  private int swatchY() { return saveY() + 24; }
  private int columns() { return Math.max(1, Math.min(16, span() / 22)); }
  private int sliderY(int channel) { return top() + 16 + channel * 18; }
  private int controlX() { return left() + 48; }
  private int controlWidth() { return span() - 48; }

  public void init() { Keyboard.enableRepeatEvents(true); }
  public void removed() { Keyboard.enableRepeatEvents(false); inline.cancel(); TextDrag.cancel(); dragging = -1; }

  private void parse() {
    try { color = LightSettings.rgb(hex.text()); error = ""; }
    catch (IllegalArgumentException e) { error = "Enter six hex digits, such as #FF5544"; }
  }

  private void slide(int x) {
    if (dragging < 0) return;
    double fraction = (x - controlX() - 4d) / Math.max(1, controlWidth() - 8);
    if (dragging == 3) localOpacity.slide(fraction);
    else {
      int channel = (int)Math.round(Math.max(0, Math.min(1, fraction)) * 255);
      channel(dragging, channel);
    }
  }

  private void channel(int index, int value) {
    int shift = (2 - index) * 8;
    color = (color & ~(255 << shift)) | value << shift;
    hex.setText(ColorSettings.hex(color));
    error = "";
  }

  private void exact(int index) {
    Setting value = index == 3 ? localOpacity : new Setting("colorPicker.channel." + index,
        "interface", "Interface", "", "Channel", "", Setting.Kind.INTEGER,
        new com.google.gson.JsonPrimitive(color >> (2 - index) * 8 & 255),
        new com.google.gson.JsonPrimitive(0), 0, 255, 1, List.of(), false);
    inline.begin(index, value, () -> { if (index < 3) channel(index, value.value.getAsInt()); },
        controlX(), sliderY(index), controlWidth(), top(), height - 32);
  }

  protected void mouseClicked(int x, int y, int button) {
    if (inline.press(x, y, button)) return;
    if (button != 0 && button != 1) return;
    dragging = -1;
    for (int i = 0; i < (opacity == null ? 3 : 4); i++) {
      if (!inside(x, y, controlX(), sliderY(i), controlWidth(), 18)) continue;
      hex.focused = false;
      if (button == 0) { dragging = i; slide(x); }
      else exact(i);
      return;
    }
    hex.focused = inside(x, y, left(), hexY(), span() - 108, 18);
    if (hex.focused) { hex.click(x, button); return; }
    if (button == 0 && inside(x, y, left() + span() - 104, hexY() - 1, 50, 20)) {
      click();
      try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ColorSettings.hex(color)), null); error = ""; }
      catch (RuntimeException e) { error = "Clipboard unavailable"; }
      return;
    }
    if (button == 0 && inside(x, y, left() + span() - 50, hexY() - 1, 50, 20)) {
      click(); hex.setText(getClipboard().strip()); parse(); return;
    }
    if (button == 0 && inside(x, y, left(), saveY(), 96, 20)) {
      click(); parse();
      if (!error.isEmpty()) return;
      String value = ColorSettings.hex(color);
      colors.removeIf(s -> s.equalsIgnoreCase(value));
      if (colors.size() == 16) colors.remove(0);
      colors.add(value);
      return;
    }
    for (int i = 0; i < colors.size(); i++) {
      if (!inside(x, y, left() + i % columns() * 22, swatchY() + i / columns() * 20, 20, 18)) continue;
      click();
      if (button == 0) { color = LightSettings.rgb(colors.get(i)); hex.setText(ColorSettings.hex(color)); error = ""; }
      else colors.remove(i);
      return;
    }
    if (button == 0 && inside(x, y, width / 2 - 102, height - 28, 100, 20)) { click(); save(); }
    if (button == 0 && inside(x, y, width / 2 + 2, height - 28, 100, 20)) { click(); minecraft.setScreen(parent); }
  }

  @Override
  protected void mouseReleased(int x, int y, int button) {
    if (button == 0) { if (Display.isActive()) slide(x); dragging = -1; }
    super.mouseReleased(x, y, button);
  }

  private void save() {
    parse();
    if (!error.isEmpty()) return;
    var beforeColor = setting.value.deepCopy();
    var beforeOpacity = opacity == null ? null : opacity.value.deepCopy();
    try {
      var savedColors = Catalog.JSON.toJsonTree(colors);
      if (draftColors != null) draftColors.validate(savedColors);
      ColorSettings.apply(setting, opacity, color, ColorSettings.opacityPercent(localOpacity));
      if (draftColors != null) draftColors.value = savedColors;
      else if (!colors.equals(originalColors)) MenuPreferences.update(v -> v.colours = new ArrayList<>(colors));
    } catch (Exception failure) {
      setting.value = beforeColor;
      if (opacity != null) opacity.value = beforeOpacity;
      error = failure.getMessage() == null ? "Could not save color" : failure.getMessage();
      return;
    }
    accepted.run();
    minecraft.setScreen(parent);
  }

  protected void keyPressed(char c, int key) {
    if (inline.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) { minecraft.setScreen(parent); return; }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { save(); return; }
    if (hex.focused) { hex.key(c, key); parse(); }
  }

  public void render(int mx, int my, float delta) {
    if (!Display.isActive()) { dragging = -1; inline.cancel(); TextDrag.cancel(); }
    if (dragging >= 0) {
      if (Mouse.isButtonDown(0)) slide(Mouse.getX() * width / minecraft.displayWidth);
      else dragging = -1;
    }
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 15, 0xffffff);
    colorSwatch(color, ColorSettings.opacityPercent(localOpacity), left(), top(), span(), 12);
    inline.beginFrame();
    String[] labels = {"Red", "Green", "Blue", "Opacity"};
    for (int i = 0; i < (opacity == null ? 3 : 4); i++) {
      text(labels[i], left(), sliderY(i) + 5, 0xdddddd);
      if (inline.editing(i)) inline.render(this, controlX(), sliderY(i), controlWidth(), mx, my, top(), height - 32);
      else {
        double value = i == 3 ? localOpacity.value.getAsDouble() : color >> (2 - i) * 8 & 255;
        double fraction = i == 3 ? (value - localOpacity.min) / Math.max(.000001, localOpacity.max - localOpacity.min) : value / 255;
        String label = i == 3 ? Setting.number(ColorSettings.opacityPercent(localOpacity)) + "%" : Integer.toString((int)value);
        slider(label, controlX(), sliderY(i), controlWidth(), mx, my, fraction);
      }
    }
    inline.endFrame();
    input(hex, left(), hexY(), span() - 108, mx, my, "#RRGGBB");
    button("Copy", left() + span() - 104, hexY() - 1, 50, 20, mx, my, true);
    button("Paste", left() + span() - 50, hexY() - 1, 50, 20, mx, my, true);
    button("Save color", left(), saveY(), 96, 20, mx, my, true);
    text(fit("Right-click removes a swatch", span() - 102), left() + 102, saveY() + 6, 0xaaaaaa);
    for (int i = 0; i < colors.size(); i++) {
      int x = left() + i % columns() * 22, y = swatchY() + i / columns() * 20;
      fill(x, y, x + 20, y + 18, 0xff000000);
      colorSwatch(LightSettings.rgb(colors.get(i)), 100, x + 1, y + 1, 18, 16);
      if (inside(mx, my, x, y, 20, 18)) tooltip(colors.get(i), mx, my);
    }
    button("Done", width / 2 - 102, height - 28, 100, 20, mx, my, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, mx, my, true);
    if (!error.isEmpty()) tooltip(fit(error, span()), left(), height - 44);
    inline.renderError(this);
  }
}
