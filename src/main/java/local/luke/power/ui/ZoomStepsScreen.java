package local.luke.power.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.map.MinimapZoomSteps;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Isolated exact zoom levels, accepted together into the owning Options session. */
public final class ZoomStepsScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final SettingsDraft draft;
  private final com.google.gson.JsonElement original;
  private String commitError = "";
  private final List<Double> values = new ArrayList<>();
  private final Deque<List<Double>> undo = new ArrayDeque<>();
  private final InlineValueEditor inline = new InlineValueEditor();
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final ScrollBar bar = new ScrollBar();
  private int scroll, dragging = -1;
  public ZoomStepsScreen(PowerOptionsScreen parent, Setting setting) {
    this.parent = parent;
    draft = new SettingsDraft(List.of(setting), s -> true); this.setting = draft.get(setting.id);
    original = setting.value.deepCopy();
    setting.value.getAsJsonArray().forEach(v -> values.add(v.getAsDouble()));
  }
  private int span() { return Math.min(420, width - 24); }
  private int left() { return (width - span()) / 2; }
  private int right() { return left() + span(); }
  private static final int TOP = 46;
  private int bottom() { return height - 36; }
  private ZoomStepsFooter footer() { return ZoomStepsFooter.at(left(), span(), textRenderer::getWidth); }
  private int valueX() { return left() + Math.min(90, span() / 3); }
  private int valueWidth() { return right() - 36 - valueX(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(right() - 4, TOP,
      Math.max(0, bottom() - TOP), values.size() * 26, scroll); }
  public void init() { inline.cancel(); dragging = -1; bar.release(); Keyboard.enableRepeatEvents(true); }
  public void removed() { inline.cancel(); Keyboard.enableRepeatEvents(false); dragging = -1; bar.release(); }
  private JsonArray json() { var out = new JsonArray(); values.forEach(out::add); return out; }
  private String error() {
    try { MinimapZoomSteps.validate(values.stream().mapToDouble(Double::doubleValue).toArray()); return ""; }
    catch (IllegalArgumentException e) { return e.getMessage(); }
  }
  private void checkpoint() {
    commitError = "";
    if (undo.isEmpty() || !undo.peek().equals(values)) {
      if (undo.size() == 64) undo.removeLast();
      undo.push(List.copyOf(values));
    }
  }
  private void save() {
    if (!error().isEmpty()) return;
    setting.value = json();
    if (parent.acceptEditor(draft)) minecraft.setScreen(parent.returnScreen());
    else commitError = parent.editorError();
  }
  private void leave() { dragging = -1; bar.release(); inline.cancel(); exit.leave(!json().equals(original), () -> minecraft.setScreen(parent.returnScreen())); }
  private void slide(int x) {
    if (dragging < 0 || dragging >= values.size()) return;
    double t = Math.max(0, Math.min(1, (x - valueX() - 4d) / Math.max(1, valueWidth() - 8)));
    values.set(dragging, Math.round((.25 + 3.75 * t) * 100) / 100d);
  }
  private void exact(int index) {
    var value = new JsonPrimitive(values.get(index));
    var entry = new Setting(setting.id + ".entry", setting.backend, setting.page, setting.group,
        "Zoom " + (index + 1), "GUI pixels per block.", Setting.Kind.DECIMAL, value, value, .25, 4, .01, List.of(), false);
    inline.begin(index, entry, () -> { checkpoint(); values.set(index, entry.value.getAsDouble()); },
        valueX(), TOP + index * 26 - scroll, valueWidth(), TOP, bottom());
  }
  protected void keyPressed(char c, int key) {
    if (exit.key(key) || inline.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) leave();
    else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) save();
  }
  public void onMouseEvent() {
    super.onMouseEvent();
    if (!exit.active() && Mouse.getEventDWheel() != 0) scroll = (int)Math.max(0, Math.min(track().maximum(), scroll - Math.signum(Mouse.getEventDWheel()) * 26));
  }
  protected void mouseReleased(int x, int y, int button) { if (button == 0) { if (org.lwjgl.opengl.Display.isActive() && !exit.active()) slide(x); dragging = -1; bar.release(); } }
  protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent.returnScreen()))) return;
    if (inline.press(x, y, button)) return;
    if (button == 0 && bar.press(track(), x, y)) { scroll = (int)bar.drag(track(), y, true); return; }
    if (y >= TOP && y < bottom()) {
      int i = (y - TOP + scroll) / 26, rowY = TOP + i * 26 - scroll;
      if (i < 0 || i >= values.size() || y >= rowY + 18) return;
      if (inside(x, y, valueX(), rowY, valueWidth(), 18)) {
        if (button == 1) exact(i);
        else if (button == 0) { checkpoint(); dragging = i; slide(x); }
      } else if (button == 0 && values.size() > 1 && inside(x, y, right() - 30, rowY, 20, 18)) {
        click(); checkpoint(); values.remove(i);
      }
      return;
    }
    if (button != 0) return;
    var footer = footer();
    int action = -1;
    for (int i = 0; i < 5; i++) if (inside(x, y, footer.x()[i], height - 28, footer.width()[i], 20)) action = i;
    if (action == 0 && values.size() < 32) {
      click(); checkpoint(); var sorted = new TreeSet<>(values); double a = .25, b = 4, gap = -1, previous = .25;
      for (double v : sorted) { if (v - previous > gap) { gap = v - previous; a = previous; b = v; } previous = v; }
      if (4 - previous > gap) { a = previous; b = 4; }
      values.add((a + b) / 2); values.sort(Double::compare); scroll = (int)Math.min(scroll, track().maximum());
    } else if (action == 1) {
      click(); checkpoint(); values.clear(); setting.defaultValue.getAsJsonArray().forEach(v -> values.add(v.getAsDouble())); scroll = 0;
    } else if (action == 2 && !undo.isEmpty()) {
      click(); values.clear(); values.addAll(undo.pop());
    } else if (action == 3) { click(); save(); }
    else if (action == 4) { click(); leave(); }
  }
  public void render(int mx, int my, float delta) {
    inline.beginFrame();
    if (!org.lwjgl.opengl.Display.isActive()) { dragging = -1; bar.release(); }
    if (!exit.active()) {
      if (dragging >= 0) { if (Mouse.isButtonDown(0)) slide(mx); else dragging = -1; }
      scroll = (int)Math.max(0, Math.min(track().maximum(), bar.drag(track(), my, Mouse.isButtonDown(0))));
    }
    renderBackground(); rectangle(0, 0, width, 38, 0x70000000);
    centeredText("Minimap zoom steps", 16, 0xffffff);
    String error = error();
    if (error.isEmpty()) error = commitError;
    if (!error.isEmpty()) text(fit(error, span()), left(), 31, 0xff8888);
    clip(left(), TOP, span(), Math.max(0, bottom() - TOP));
    for (int i = 0; i < values.size(); i++) {
      int y = TOP + i * 26 - scroll;
      if (y + 18 <= TOP || y >= bottom()) continue;
      text("Zoom " + (i + 1), left(), y + 5, 0xd0d0d0);
      if (inline.editing(i)) inline.render(this, valueX(), y, valueWidth(), mx, my, TOP, bottom());
      else slider(java.math.BigDecimal.valueOf(values.get(i)).stripTrailingZeros().toPlainString() + "x", valueX(), y, valueWidth(), mx, my, (values.get(i) - .25) / 3.75);
      button("-", right() - 30, y, 20, 18, mx, my, values.size() > 1);
    }
    inline.endFrame(); unclip(); bar.render(this, track());
    rectangle(0, height - 36, width, height, 0x70000000);
    var footer = footer();
    boolean[] enabled = {values.size() < 32, true, !undo.isEmpty(), error().isEmpty(), true};
    for (int i = 0; i < 5; i++) button(ZoomStepsFooter.LABELS[i], footer.x()[i], height - 28,
        footer.width()[i], 20, mx, my, enabled[i]);
    exit.render(this, "Save zoom step changes?", mx, my);
  }
}
