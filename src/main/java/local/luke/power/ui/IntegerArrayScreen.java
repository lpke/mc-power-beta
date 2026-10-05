package local.luke.power.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.stream.StreamSupport;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Ordered numeric entries with sliders, exact values and an isolated, cancellable draft. */
public final class IntegerArrayScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final IntegerArrayDraft.Rules rules;
  private final IntegerArrayDraft draft;
  private final String entryLabel, unit;
  private final InlineValueEditor valueEditor = new InlineValueEditor();
  private final ScrollBar scrollbar = new ScrollBar();
  private int scroll, dragging = -1;

  public IntegerArrayScreen(PowerOptionsScreen parent, Setting setting,
      IntegerArrayDraft.Rules rules, String entryLabel, String unit) {
    this.parent = parent;
    this.setting = setting;
    this.rules = rules;
    this.entryLabel = entryLabel;
    this.unit = unit;
    draft = new IntegerArrayDraft(rules, values(setting.value));
  }

  private static List<Integer> values(JsonElement value) {
    return StreamSupport.stream(value.getAsJsonArray().spliterator(), false)
        .map(v -> v.getAsBigDecimal().intValueExact()).toList();
  }

  private int span() { return Math.min(480, width - 24); }
  private int left() { return (width - span()) / 2; }
  private int right() { return left() + span(); }
  private int bottom() { return height - 62; }
  private int sliderX() { return left() + Math.min(100, span() / 4); }
  private int sliderWidth() { return right() - 68 - sliderX(); }
  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() - 3, 70, Math.max(0, bottom() - 70), draft.size() * 26, scroll);
  }

  public void init() { valueEditor.cancel(); Keyboard.enableRepeatEvents(true); }

  public void removed() {
    valueEditor.cancel();
    Keyboard.enableRepeatEvents(false);
    dragging = -1;
    scrollbar.release();
  }

  private void slide(int x) {
    if (dragging < 0 || dragging >= draft.size()) return;
    double fraction = Math.max(0, Math.min(1, (x - sliderX() - 4d) / Math.max(1, sliderWidth() - 8)));
    draft.set(dragging, rules.min() + (int) Math.round(fraction * (rules.max() - rules.min())));
  }

  private void exact(int index) {
    var value = new JsonPrimitive(draft.get(index));
    var entry = new Setting(setting.id + ".entry", setting.backend, setting.page, setting.group,
        entryLabel + " " + (index + 1), "Distance in " + unit + ".", Setting.Kind.INTEGER,
        value, value, rules.min(), rules.max(), 1, List.of(), false);
    dragging = -1;
    valueEditor.begin(index, entry, () -> draft.set(index, entry.value.getAsInt()),
        sliderX(), 70 + index * 26 - scroll, sliderWidth(), 70, bottom());
  }

  private void save() {
    if (!draft.error().isEmpty()) return;
    var value = new JsonArray();
    draft.values().forEach(value::add);
    setting.value = value;
    parent.changed(setting);
    minecraft.setScreen(parent);
  }

  protected void keyPressed(char c, int key) {
    if (valueEditor.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) minecraft.setScreen(parent);
    else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) save();
  }

  public void onMouseEvent() {
    super.onMouseEvent();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0 && dragging < 0) {
      valueEditor.cancel();
      scroll = (int) Math.max(0, Math.min(track().maximum(), scroll - Math.signum(wheel) * 26));
    }
  }

  protected void mouseClicked(int x, int y, int button) {
    if (valueEditor.press(x, y, button)) return;
    if (button != 0 && button != 1) return;
    if (button == 0 && scrollbar.press(track(), x, y)) {
      scroll = (int) scrollbar.drag(track(), y, true);
      return;
    }
    if (y >= 70 && y < bottom()) {
      int index = (y - 70 + scroll) / 26;
      int rowY = 70 + index * 26 - scroll;
      if (index < 0 || index >= draft.size() || y >= rowY + 18) return;
      if (inside(x, y, sliderX(), rowY, sliderWidth(), 18)) {
        if (button == 0) { dragging = index; slide(x); }
        else exact(index);
      } else if (button == 0) {
        if (inside(x, y, right() - 64, rowY, 16, 18)) draft.move(index, -1);
        else if (inside(x, y, right() - 46, rowY, 16, 18)) draft.move(index, 1);
        else if (inside(x, y, right() - 28, rowY, 18, 18)) draft.remove(index);
      }
      return;
    }
    if (button != 0) return;
    if (inside(x, y, left(), height - 54, 100, 20)) {
      if (draft.add()) scroll = (int) track().maximum();
    } else if (inside(x, y, left() + 104, height - 54, 76, 20)) {
      draft.reset(values(setting.defaultValue));
      scroll = 0;
    } else if (inside(x, y, width / 2 - 102, height - 28, 100, 20)) save();
    else if (inside(x, y, width / 2 + 2, height - 28, 100, 20)) minecraft.setScreen(parent);
  }

  public void render(int mx, int my, float delta) {
    valueEditor.beginFrame();
    if (dragging >= 0) {
      if (Mouse.isButtonDown(0)) slide(mx);
      else dragging = -1;
    }
    scroll = (int) Math.max(0, Math.min(track().maximum(), scrollbar.drag(track(), my, Mouse.isButtonDown(0))));
    renderBackground();
    drawCenteredTextWithShadow(textRenderer, setting.label, width / 2, 16, 0xffffff);
    text(fit("F cycles in this order; Shift+F reverses.", span()), left(), 36, 0xaaaaaa);
    String error = draft.error();
    text(fit(error.isEmpty() ? "Use 1 to " + rules.limit() + " different distances, " + rules.min()
        + " to " + rules.max() + " " + unit + "." : error, span()), left(), 52,
        error.isEmpty() ? 0xaaaaaa : 0xff8888);
    clip(left(), 70, span(), Math.max(0, bottom() - 70));
    String tip = "";
    for (int i = 0; i < draft.size(); i++) {
      int y = 70 + i * 26 - scroll;
      if (y + 18 <= 70 || y >= bottom()) continue;
      text(entryLabel + " " + (i + 1), left(), y + 5, 0xd0d0d0);
      if (valueEditor.editing(i)) valueEditor.render(this, sliderX(), y, sliderWidth(), mx, my, 70, bottom());
      else slider(draft.get(i) + " " + unit, sliderX(), y, sliderWidth(), mx, my,
          (draft.get(i) - rules.min()) / (double) Math.max(1, rules.max() - rules.min()));
      iconButton("up", right() - 64, y, 16, mx, my, i > 0);
      iconButton("down", right() - 46, y, 16, mx, my, i + 1 < draft.size());
      button("-", right() - 28, y, 18, 18, mx, my, draft.size() > 1);
      if (my >= 70 && my < bottom()) {
        if (inside(mx, my, sliderX(), y, sliderWidth(), 18))
          tip = valueEditor.editing(i) ? valueEditor.help() : "Right-click to enter an exact distance.";
        else if (inside(mx, my, right() - 64, y, 16, 18)) tip = "Move earlier in the cycle";
        else if (inside(mx, my, right() - 46, y, 16, 18)) tip = "Move later in the cycle";
        else if (inside(mx, my, right() - 28, y, 18, 18)) tip = "Remove this distance";
      }
    }
    valueEditor.endFrame();
    unclip();
    scrollbar.render(this, track());
    button("Add distance", left(), height - 54, 100, 20, mx, my, draft.size() < rules.limit());
    button("Defaults", left() + 104, height - 54, 76, 20, mx, my, true);
    button("Done", width / 2 - 102, height - 28, 100, 20, mx, my, error.isEmpty());
    button("Cancel", width / 2 + 2, height - 28, 100, 20, mx, my, true);
    if (!tip.isEmpty()) tooltip(tip, mx, my);
  }
}
