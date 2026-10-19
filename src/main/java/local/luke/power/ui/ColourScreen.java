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
import org.lwjgl.opengl.GL11;

/** RGB, opacity and saved swatches remain private until Done accepts the whole edit. */
public final class ColourScreen extends UiScreen {
  private static final int ALPHA = 3, BRIGHTNESS = 4, WHEEL = 5, ROW = 22, PREVIEW = 36;
  private static final int[] RGB_CONTROLS = {0, 1, 2}, RGBA_CONTROLS = {0, 1, 2, ALPHA};
  private static final int[] WHEEL_CONTROLS = {BRIGHTNESS}, WHEEL_ALPHA_CONTROLS = {BRIGHTNESS, ALPHA};
  private static final String[] LABELS = {"Red", "Green", "Blue", "Opacity", "Brightness"};
  private static final WheelVertex[] WHEEL_MESH = wheelMesh();
  private final net.minecraft.client.gui.screen.Screen parent;
  private final Runnable accepted;
  private final Setting setting, opacity, draftColors;
  private final TextInput hex;
  private final InlineValueEditor inline = new InlineValueEditor();
  private final ScrollBar scrollbar = new ScrollBar();
  private final List<String> colors;
  private final List<String> originalColors;
  private final Setting localOpacity;
  private final ColorPickerSnapshot original;
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final double[] scrollPositions = new double[2];
  private ColorWheel.Hsv hsv;
  private int color, dragging = -1, tab;
  private double scroll;
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
    hsv = ColorWheel.fromRgb(color, null);
    hex = new TextInput(ColorSettings.hex(color), 7);
    localOpacity = opacity == null ? null : new Setting(opacity.id, opacity.backend, opacity.page, opacity.group,
        opacity.label, opacity.description, opacity.kind, opacity.value, opacity.powerBetaDefault,
        opacity.min, opacity.max, opacity.step, opacity.choices, opacity.restart);
    this.colors = new ArrayList<>();
    if (colors == null) this.colors.addAll(MenuPreferences.current().colours);
    else colors.value.getAsJsonArray().forEach(value -> this.colors.add(value.getAsString()));
    originalColors = List.copyOf(this.colors);
    original = new ColorPickerSnapshot(color, ColorSettings.opacityPercent(localOpacity), this.colors);
  }

  public net.minecraft.client.gui.screen.Screen parentScreen() { return parent; }

  public static boolean accepts(Setting setting) { return ColorSettings.accepts(setting); }
  private int left() { return (width - span()) / 2; }
  private int span() { return Math.min(340, width - 24); }
  private int bodyWidth() { return span() - 10; }
  private int previewY() { return 34; }
  private int tabsY() { return previewY() + PREVIEW + 6; }
  private int tabWidth() { return (span() - 4) / 2; }
  private int bodyTop() { return tabsY() + 24; }
  private int bodyBottom() { return height - 34; }
  private int bodyY() { return bodyTop() - (int)scroll; }
  private int wheelDiameter() { return Math.min(112, Math.max(48, bodyBottom() - bodyTop())); }
  private int[] controls() {
    return tab == 1 ? opacity == null ? RGB_CONTROLS : RGBA_CONTROLS
        : opacity == null ? WHEEL_CONTROLS : WHEEL_ALPHA_CONTROLS;
  }
  private int sectionHeight() { return tab == 1 ? controls().length * ROW - 4 : wheelDiameter(); }
  private int hexY() { return bodyY() + sectionHeight() + 8; }
  private int saveY() { return hexY() + 24; }
  private int swatchY() { return saveY() + 24; }
  private int columns() { return Math.max(1, Math.min(16, bodyWidth() / 22)); }
  private int sliderY(int channel) {
    return tab == 1 ? bodyY() + channel * ROW
        : bodyY() + (wheelDiameter() - (controls().length * ROW - 4)) / 2 + (channel == ALPHA ? ROW : 0);
  }
  private int labelX() { return tab == 1 ? left() : left() + wheelDiameter() + 12; }
  private int controlX() { return labelX() + (tab == 1 ? 48 : 64); }
  private int controlWidth() { return left() + bodyWidth() - controlX(); }
  private ColorWheel.Bounds wheelBounds() {
    double radius = wheelDiameter() / 2d;
    return new ColorWheel.Bounds(left() + radius, bodyY() + radius, radius - 2);
  }
  private ScrollBar.Track track() {
    int rows = (colors.size() + columns() - 1) / columns();
    int content = sectionHeight() + 56 + rows * ROW;
    return new ScrollBar.Track(left() + span() - 4, bodyTop(), Math.max(0, bodyBottom() - bodyTop()), content, scroll);
  }
  private boolean inBody(int x, int y) { return inside(x, y, left(), bodyTop(), bodyWidth(), Math.max(0, bodyBottom() - bodyTop())); }

  public void init() { Keyboard.enableRepeatEvents(true); stopGesture(); inline.cancel(); }
  public void removed() { Keyboard.enableRepeatEvents(false); stopGesture(); inline.cancel(); TextDrag.cancel(); }

  private void stopGesture() { dragging = -1; scrollbar.release(); }
  private boolean changed() {
    boolean invalid = false;
    try { LightSettings.rgb(hex.text()); } catch (IllegalArgumentException e) { invalid = true; }
    return original.changed(color, ColorSettings.opacityPercent(localOpacity), colors, invalid);
  }
  private void leave() {
    stopGesture(); inline.cancel(); hex.focused = false;
    exit.leave(changed(), () -> minecraft.setScreen(parent));
  }

  private void parse() {
    try { color = LightSettings.rgb(hex.text()); hsv = ColorWheel.fromRgb(color, hsv); error = ""; }
    catch (IllegalArgumentException e) { error = "Enter six hex digits, such as #FF5544"; }
  }

  private void changedColor(int rgb) {
    color = rgb;
    hex.setText(ColorSettings.hex(color));
    error = "";
  }

  private void drag(int x, int y) {
    if (dragging < 0) return;
    if (dragging == WHEEL) {
      hsv = wheelBounds().select(hsv, x, y);
      changedColor(hsv.rgb());
      return;
    }
    double fraction = Math.max(0, Math.min(1, (x - controlX() - 4d) / Math.max(1, controlWidth() - 8)));
    if (dragging == ALPHA) localOpacity.slide(fraction);
    else if (dragging == BRIGHTNESS) { hsv = hsv.brightness(fraction); changedColor(hsv.rgb()); }
    else channel(dragging, (int)Math.round(fraction * 255));
  }

  private void channel(int index, int value) {
    int shift = (2 - index) * 8;
    changedColor((color & ~(255 << shift)) | value << shift);
    hsv = ColorWheel.fromRgb(color, hsv);
  }

  private void exact(int index) {
    Setting value = index == ALPHA ? localOpacity : new Setting("colorPicker.channel." + index,
        "interface", "Interface", "", LABELS[index], "", index == BRIGHTNESS ? Setting.Kind.DECIMAL : Setting.Kind.INTEGER,
        new com.google.gson.JsonPrimitive(index == BRIGHTNESS ? hsv.brightness() * 100 : color >> (2 - index) * 8 & 255),
        new com.google.gson.JsonPrimitive(0), 0, index == BRIGHTNESS ? 100 : 255,
        index == BRIGHTNESS ? .1 : 1, List.of(), false);
    inline.begin(index, value, () -> {
      if (index < ALPHA) channel(index, value.value.getAsInt());
      else if (index == BRIGHTNESS) { hsv = hsv.brightness(value.value.getAsDouble() / 100); changedColor(hsv.rgb()); }
    }, controlX(), sliderY(index), controlWidth(), bodyTop(), bodyBottom());
  }

  private void selectTab(int selected) {
    click();
    stopGesture(); inline.cancel(); hex.focused = false;
    scrollPositions[tab] = scroll;
    tab = selected;
    scroll = Math.min(track().maximum(), scrollPositions[tab]);
  }

  protected void mouseClicked(int x, int y, int button) {
    if (!Display.isActive() || minecraft.currentScreen != this) { stopGesture(); return; }
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent))) return;
    if (dragging >= 0 || inline.press(x, y, button)) return;
    if (button != 0 && button != 1) return;
    if (button == 0) for (int i = 0; i < 2; i++) {
      if (inside(x, y, left() + i * (tabWidth() + 4), tabsY(), tabWidth(), 18)) { selectTab(i); return; }
    }
    if (button == 0 && inside(x, y, width / 2 - 102, height - 28, 100, 20)) { click(); save(); return; }
    if (button == 0 && inside(x, y, width / 2 + 2, height - 28, 100, 20)) { click(); leave(); return; }
    if (button == 0 && scrollbar.press(track(), x, y)) {
      hex.focused = false;
      scroll = scrollbar.drag(track(), y, true);
      return;
    }
    boolean body = inBody(x, y);
    if (body) {
      if (button == 0 && tab == 0 && wheelBounds().contains(x, y)) {
        hex.focused = false; dragging = WHEEL; drag(x, y); return;
      }
      for (int i : controls()) {
        if (!inside(x, y, controlX(), sliderY(i), controlWidth(), 18)) continue;
        hex.focused = false;
        if (button == 0) { dragging = i; drag(x, y); }
        else exact(i);
        return;
      }
    }
    hex.focused = body && inside(x, y, left(), hexY(), bodyWidth() - 108, 18);
    if (hex.focused) { hex.click(x, button); return; }
    if (!body) return;
    if (button == 0 && inside(x, y, left() + bodyWidth() - 104, hexY() - 1, 50, 20)) {
      click();
      try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ColorSettings.hex(color)), null); error = ""; }
      catch (RuntimeException e) { error = "Clipboard unavailable"; }
      return;
    }
    if (button == 0 && inside(x, y, left() + bodyWidth() - 50, hexY() - 1, 50, 20)) {
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
      if (!inside(x, y, left() + i % columns() * 22, swatchY() + i / columns() * ROW, 20, 18)) continue;
      click();
      if (button == 0) { changedColor(LightSettings.rgb(colors.get(i))); hsv = ColorWheel.fromRgb(color, hsv); }
      else colors.remove(i);
      return;
    }
  }

  @Override
  protected void mouseReleased(int x, int y, int button) {
    if (button == 0) { if (!exit.active() && Display.isActive() && minecraft.currentScreen == this) drag(x, y); stopGesture(); }
    super.mouseReleased(x, y, button);
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    if (minecraft.currentScreen != this || !Display.isActive() || exit.active()) { stopGesture(); return; }
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if (dragging >= 0) { drag(x, y); return; }
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0 && inBody(x, y)) {
      inline.cancel(); hex.focused = false;
      scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(wheel) * ROW * 2));
    }
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
    if (exit.key(key)) return;
    if (inline.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) { leave(); return; }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { save(); return; }
    if (hex.focused) { hex.key(c, key); parse(); }
  }

  private void renderControl(int channel, int mx, int my) {
    int y = sliderY(channel);
    if (y + 18 <= bodyTop() || y >= bodyBottom()) return;
    text(LABELS[channel], labelX(), y + 5, 0xdddddd);
    if (inline.editing(channel)) inline.render(this, controlX(), y, controlWidth(), mx, my, bodyTop(), bodyBottom());
    else {
      double value = channel == ALPHA ? localOpacity.value.getAsDouble()
          : channel == BRIGHTNESS ? hsv.brightness() : color >> (2 - channel) * 8 & 255;
      double fraction = channel == ALPHA ? (value - localOpacity.min) / Math.max(.000001, localOpacity.max - localOpacity.min)
          : channel == BRIGHTNESS ? value : value / 255;
      String label = channel == ALPHA ? Setting.number(ColorSettings.opacityPercent(localOpacity)) + "%"
          : channel == BRIGHTNESS ? Setting.number(Math.round(value * 1000) / 10d) + "%" : Integer.toString((int)value);
      slider(label, controlX(), y, controlWidth(), mx, my, fraction);
    }
  }

  private record WheelVertex(double x, double y, float red, float green, float blue) {}

  private static WheelVertex[] wheelMesh() {
    WheelVertex[] vertices = new WheelVertex[193];
    for (int i = 0; i < vertices.length; i++) {
      double hue = i / 192d, angle = hue * Math.PI * 2;
      int rgb = new ColorWheel.Hsv(hue, 1, 1).rgb();
      vertices[i] = new WheelVertex(Math.cos(angle), Math.sin(angle), (rgb >> 16 & 255) / 255f,
          (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
    }
    return vertices;
  }

  private void renderWheel() {
    ColorWheel.Bounds bounds = wheelBounds();
    float brightness = (float)hsv.brightness();
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_LINE_BIT);
    try {
      GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_BLEND);
      GL11.glShadeModel(GL11.GL_SMOOTH);
      GL11.glBegin(GL11.GL_TRIANGLE_FAN);
      GL11.glColor3f(brightness, brightness, brightness);
      GL11.glVertex2d(bounds.x(), bounds.y());
      for (WheelVertex vertex : WHEEL_MESH) {
        GL11.glColor3f(vertex.red * brightness, vertex.green * brightness, vertex.blue * brightness);
        GL11.glVertex2d(bounds.x() + vertex.x * bounds.radius(), bounds.y() + vertex.y * bounds.radius());
      }
      GL11.glEnd();
      GL11.glColor3f(.7f, .7f, .7f);
      GL11.glLineWidth(1);
      GL11.glBegin(GL11.GL_LINE_LOOP);
      for (int i = 0; i < WHEEL_MESH.length - 1; i++) {
        WheelVertex vertex = WHEEL_MESH[i];
        GL11.glVertex2d(bounds.x() + vertex.x * (bounds.radius() + 1), bounds.y() + vertex.y * (bounds.radius() + 1));
      }
      GL11.glEnd();
    } finally { GL11.glPopAttrib(); }
    ColorWheel.Point marker = bounds.marker(hsv);
    int x = (int)Math.round(marker.x()), y = (int)Math.round(marker.y());
    fill(x - 3, y - 3, x + 4, y + 4, 0xff000000);
    fill(x - 2, y - 2, x + 3, y + 3, 0xffffffff);
    fill(x - 1, y - 1, x + 2, y + 2, 0xff000000 | color);
  }

  public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    if (exit.active()) { mx = my = -1000; }
    if (!Display.isActive()) { stopGesture(); inline.cancel(); TextDrag.cancel(); }
    if (dragging >= 0) {
      if (Mouse.isButtonDown(0)) drag(Mouse.getX() * width / minecraft.displayWidth,
          height - Mouse.getY() * height / minecraft.displayHeight - 1);
      // Native release owns the final coordinate; polling can lead queued events.
    }
    scroll = Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Display.isActive() && Mouse.isButtonDown(0))));
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, fit(setting.label, width - 16), width / 2, 15, 0xffffff);
    int previewX = (width - PREVIEW) / 2;
    fill(previewX - 1, previewY() - 1, previewX + PREVIEW + 1, previewY() + PREVIEW + 1, 0xffaaaaaa);
    colorSwatch(color, ColorSettings.opacityPercent(localOpacity), previewX, previewY(), PREVIEW, PREVIEW);
    tabButton("Color wheel", left(), tabsY(), tabWidth(), mx, my, tab == 0);
    tabButton("RGBA", left() + tabWidth() + 4, tabsY(), tabWidth(), mx, my, tab == 1);
    inline.beginFrame();
    int listMouseY = inBody(mx, my) ? my : Integer.MIN_VALUE;
    clip(left() - 1, bodyTop(), bodyWidth() + 2, Math.max(0, bodyBottom() - bodyTop()));
    if (tab == 0) renderWheel();
    for (int i : controls()) renderControl(i, mx, listMouseY);
    input(hex, left(), hexY(), bodyWidth() - 108, mx, listMouseY, "#RRGGBB");
    button("Copy", left() + bodyWidth() - 104, hexY() - 1, 50, 20, mx, listMouseY, true);
    button("Paste", left() + bodyWidth() - 50, hexY() - 1, 50, 20, mx, listMouseY, true);
    button("Save color", left(), saveY(), 96, 20, mx, listMouseY, true);
    text(fit("Right-click removes a swatch", bodyWidth() - 102), left() + 102, saveY() + 6, 0xaaaaaa);
    String tip = "";
    for (int i = 0; i < colors.size(); i++) {
      int x = left() + i % columns() * 22, y = swatchY() + i / columns() * ROW;
      fill(x, y, x + 20, y + 18, 0xff000000);
      colorSwatch(LightSettings.rgb(colors.get(i)), 100, x + 1, y + 1, 18, 16);
      if (inside(mx, listMouseY, x, y, 20, 18)) tip = colors.get(i);
    }
    unclip();
    inline.endFrame();
    scrollbar.render(this, track());
    button("Done", width / 2 - 102, height - 28, 100, 20, mx, my, true);
    button("Cancel", width / 2 + 2, height - 28, 100, 20, mx, my, true);
    if (!tip.isEmpty()) tooltip(tip, mx, my);
    if (!error.isEmpty()) tooltip(fit(error, span()), left(), height - 44);
    inline.renderError(this);
    exit.render(this, "Save color changes?", pointerX, pointerY);
  }
}
