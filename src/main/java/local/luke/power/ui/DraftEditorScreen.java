package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Shared draft controls, inline input, bounded scrolling and modal exit handling. */
abstract class DraftEditorScreen extends UiScreen {
  protected final PowerOptionsScreen parent;
  protected final SettingsDraft draft;
  protected final DraftExitPrompt exit = new DraftExitPrompt();
  protected final InlineValueEditor inline = new InlineValueEditor();
  protected String error = "";
  private final ScrollBar bar = new ScrollBar();
  private double scroll;
  private Setting slider;
  private boolean resetPrompt;
  private final Set<String> collapsed = new HashSet<>();

  DraftEditorScreen(PowerOptionsScreen parent, SettingsDraft draft) { this.parent = parent; this.draft = draft; }
  protected int left() { return (width - span()) / 2; }
  protected int span() { return Math.min(760, width - 20); }
  protected int right() { return left() + span(); }
  protected int controlsTop() { return 55; }
  protected int controlsBottom() { return Math.min(178, Math.max(controlsTop() + 23, height * 43 / 100)); }
  protected int previewTop() { return controlsBottom() + 12; }
  protected int previewBottom() { return height - 53; }
  protected abstract String title();
  protected abstract List<Setting> controls();
  protected abstract void renderToolbar(int x, int y);
  protected abstract void renderPreview(int x, int y, float delta);
  protected void beforeRender(int x, int y) {}
  protected boolean toolbarClick(int x, int y, int button) { return false; }
  protected void previewClick(int x, int y, int button) {}
  protected void stopGesture() {}
  protected void finishGesture(int x, int y, int button) {}
  protected boolean editorKey(char c, int key) { return false; }
  protected String label(Setting s) { return s.label; }
  protected String display(Setting s) { return s.display(); }
  protected boolean usesSlider(Setting s) { return s.slider(); }
  protected boolean resettable(Setting s) { return false; }
  protected String controlGroup(Setting s) { return ""; }
  protected String groupKey(String group) { return group; }
  protected boolean offersPositionReset() { return false; }
  protected List<Setting> positionSettings() { return List.of(); }
  protected List<Setting> allResetSettings() { return draft.settings(); }
  protected String resetSubject() { return "Selected"; }
  protected void reset() { controls().forEach(Setting::reset); }
  protected void resetFinished() {}
  protected void afterEdit() {}
  protected void valueChanged(Setting s) { afterEdit(); }
  protected String hint() { return "Click an element to edit it. Drag to move. Arrow keys fine-tune."; }
  protected String resetLabel() { return "Reset..."; }
  protected String resetQuestion() { return "Reset to defaults?"; }
  protected String extraFooterLabel() { return ""; }
  protected void extraFooterAction() {}
  protected void controlsChanged() { scroll = 0; bar.release(); slider = null; inline.cancel(); }
  protected boolean modal() { return exit.active() || resetPrompt; }
  protected int controlX() { return left() + span() / 2; }
  protected int controlWidth() { return span() / 2 - 10; }
  protected int controlWidth(Setting s) { return controlWidth() - (resettable(s) ? 24 : 0); }
  private int resetX() { return controlX() + controlWidth() - 20; }
  private record ControlRow(Setting setting, String heading, int y, int height) {}
  private List<ControlRow> controlRows() {
    List<ControlRow> rows = new ArrayList<>(); String previous = ""; int y = 0;
    for (Setting setting : controls()) {
      String group = controlGroup(setting);
      if (!group.isEmpty() && !group.equals(previous)) { rows.add(new ControlRow(null, group, y, SettingsGroup.HEIGHT)); y += SettingsGroup.HEIGHT; }
      if (group.isEmpty() || !collapsed.contains(groupKey(group))) {
        rows.add(new ControlRow(setting, "", y, 23)); y += 23;
      }
      previous = group;
    }
    return rows;
  }
  private StickyHeader stickyHeader(List<ControlRow> rows) {
    return StickyHeader.at(rows, row -> row.setting == null, row -> row.setting != null,
        ControlRow::y, ControlRow::height, (int)scroll, controlsTop(), controlsBottom());
  }
  private void groupHeading(ControlRow row, int y) {
    SettingsGroup.draw(this, fit(row.heading, span() - 24), collapsed.contains(groupKey(row.heading)), left() + 4, right() - 10, y);
  }
  protected int controlY(Setting setting) {
    return controlRows().stream().filter(row -> row.setting == setting).findFirst()
        .map(row -> controlsTop() + row.y - (int)scroll).orElse(controlsTop());
  }
  private ScrollBar.Track track() {
    List<ControlRow> rows = controlRows();
    int content = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y + rows.get(rows.size() - 1).height;
    return new ScrollBar.Track(right() - 4, controlsTop(), Math.max(0, controlsBottom() - controlsTop()), content, scroll);
  }
  public void init() { Keyboard.enableRepeatEvents(true); inline.cancel(); stopGesture(); }
  public void removed() { Keyboard.enableRepeatEvents(false); stopGesture(); slider = null; bar.release(); inline.cancel(); TextDrag.cancel(); }
  protected void save() {
    stopGesture(); slider = null; bar.release(); inline.cancel();
    if (parent.acceptEditor(draft)) minecraft.setScreen(parent); else error = parent.editorError();
  }
  protected void leave() {
    stopGesture(); slider = null; bar.release(); inline.cancel();
    exit.leave(draft.changed(), () -> minecraft.setScreen(parent));
  }
  protected void openInline(Setting s, Runnable accepted, int x, int y, int w) {
    inline.begin(s, s, accepted, x, y, w, controlsTop(), controlsBottom());
  }
  protected void edit(Setting s, int direction, int y) {
    draft.checkpoint(); error = "";
    if (s.kind == Setting.Kind.BOOLEAN || s.kind == Setting.Kind.CHOICE) { s.cycle(direction); valueChanged(s); }
    else if (ColourScreen.accepts(s)) minecraft.setScreen(new ColourScreen(this, s, () -> valueChanged(s)));
    else if (s.numeric() || s.kind == Setting.Kind.TEXT)
      openInline(s, () -> valueChanged(s), controlX(), y, controlWidth(s));
    else minecraft.setScreen(new ValueScreen(s, this, () -> valueChanged(s)));
  }
  private void slide(int x) {
    if (slider == null) return;
    slider.slide((x - controlX() - 4d) / Math.max(1, controlWidth(slider) - 8)); valueChanged(slider);
  }
  protected void keyPressed(char c, int key) {
    if (exit.key(key)) return;
    if (resetPrompt) { if (key == Keyboard.KEY_ESCAPE) resetPrompt = false; return; }
    if (inline.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE) { leave(); return; }
    if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && key == Keyboard.KEY_Z) {
      stopGesture(); draft.undo(); afterEdit(); return;
    }
    editorKey(c, key);
  }
  public void onMouseEvent() {
    super.onMouseEvent();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    int wheel = Mouse.getEventDWheel();
    if (!modal() && wheel != 0 && inside(x, y, left(), controlsTop(), span(), controlsBottom() - controlsTop()))
      scroll = Math.max(0, Math.min(track().maximum(), scroll - Math.signum(wheel) * 46));
  }
  private record Footer(String label, int x, int width, Runnable action, boolean enabled) {}
  private List<Footer> footer() {
    List<String> labels = new ArrayList<>(List.of("Undo", resetLabel()));
    if (!extraFooterLabel().isEmpty()) labels.add(extraFooterLabel());
    labels.addAll(List.of("Cancel", "Done"));
    int[] sizes = new int[labels.size()]; int total = 0;
    for (int i = 0; i < sizes.length; i++) { sizes[i] = Math.max(36, textRenderer.getWidth(labels.get(i)) + 12); total += sizes[i]; }
    int room = span() - (sizes.length - 1) * 4;
    if (total > room) for (int i = 0; i < sizes.length; i++) sizes[i] = Math.max(10, sizes[i] * room / total);
    else {
      int extra = Math.min(76, (room - total) / sizes.length);
      for (int i = 0; i < sizes.length; i++) sizes[i] += extra;
    }
    List<Footer> buttons = new ArrayList<>(); int x = left();
    for (int i = 0; i < sizes.length; i++) {
      if (i == sizes.length - 2) x = right() - sizes[i] - sizes[i + 1] - 4;
      String label = labels.get(i);
      Runnable action = i == 0 ? () -> { stopGesture(); draft.undo(); afterEdit(); }
          : i == 1 ? () -> { stopGesture(); resetPrompt = true; }
          : i == sizes.length - 2 ? this::leave : i == sizes.length - 1 ? this::save : this::extraFooterAction;
      buttons.add(new Footer(label, x, sizes[i], action, i != 0 || draft.canUndo())); x += sizes[i] + 4;
    }
    return buttons;
  }
  private void performReset(boolean positionOnly, boolean all) {
    stopGesture(); slider = null; bar.release(); inline.cancel(); error = "";
    draft.checkpoint();
    if (offersPositionReset()) {
      List<Setting> scope = all ? allResetSettings() : controls();
      scope.stream().filter(s -> !positionOnly || HudEditorSections.position(s)).forEach(Setting::reset);
    } else reset();
    afterEdit(); resetFinished(); resetPrompt = false;
  }
  private record ResetButton(String label, int x, int y, int width, Runnable action, boolean enabled) {}
  private List<ResetButton> resetButtons() {
    List<ResetButton> buttons = new ArrayList<>();
    int w = Math.min(220, (width - 28) / 2), top = height / 2 - 16;
    if (offersPositionReset()) {
      List<String> labels = List.of("All settings", "All positions", resetSubject() + " settings", resetSubject() + " positions");
      for (int i = 0; i < labels.size(); i++) {
        boolean positions = i % 2 == 1, all = i < 2;
        List<Setting> scope = all ? allResetSettings() : controls();
        boolean enabled = scope.stream().anyMatch(s -> (!positions || HudEditorSections.position(s)) && !s.value.equals(s.defaultValue));
        buttons.add(new ResetButton(labels.get(i), width / 2 - w - 2 + i % 2 * (w + 4), top + i / 2 * 24,
            w, () -> performReset(positions, all), enabled));
      }
    } else buttons.add(new ResetButton("Reset", (width - w) / 2, top, w, () -> performReset(false, false), true));
    buttons.add(new ResetButton("Cancel", (width - w) / 2, top + (offersPositionReset() ? 48 : 24), w, () -> resetPrompt = false, true));
    return buttons;
  }
  protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent))) return;
    if (resetPrompt) {
      if (button == 0) for (ResetButton f : resetButtons()) if (inside(x, y, f.x, f.y, f.width, 20)) {
        if (f.enabled) f.action.run(); break;
      }
      return;
    }
    if (inline.press(x, y, button)) return;
    slider = null; bar.release();
    if (toolbarClick(x, y, button)) return;
    if (button == 0 && bar.press(track(), x, y)) return;
    if ((button == 0 || button == 1) && inside(x, y, left(), controlsTop(), span() - 9, controlsBottom() - controlsTop())) {
      List<ControlRow> rows = controlRows(); StickyHeader sticky = stickyHeader(rows);
      int pinned = sticky != null && sticky.contains(y, controlsTop()) ? sticky.index() : -1;
      for (int i = 0; i < rows.size(); i++) {
        if (pinned >= 0 && pinned != i) continue;
        ControlRow row = rows.get(i);
        int rowY = pinned == i ? sticky.y() : controlsTop() + row.y - (int)scroll;
        if (y < rowY || y >= rowY + row.height) continue;
        if (row.setting == null) {
          if (button == 0) {
            stopGesture(); inline.cancel(); SettingsGroup.toggle(collapsed, groupKey(row.heading));
            if (pinned >= 0) scroll = row.y;
            scroll = Math.min(scroll, track().maximum());
          }
          return;
        }
        int rowsTop = sticky == null ? controlsTop() : Math.max(controlsTop(), sticky.y() + sticky.height());
        if (rowY < rowsTop || rowY + 18 > controlsBottom() || y >= rowY + 18 || x < controlX()) continue;
        Setting s = row.setting;
        if (resettable(s) && x >= resetX()) {
          if (button == 0 && inside(x, y, resetX(), rowY, 20, 18) && !s.value.equals(s.defaultValue)) {
            draft.checkpoint(); error = ""; s.reset(); valueChanged(s);
          }
          return;
        }
        if (x >= controlX() + controlWidth(s)) return;
        if (button == 0 && usesSlider(s)) { draft.checkpoint(); slider = s; slide(x); }
        else edit(s, button == 1 ? -1 : 1, rowY);
        break;
      }
      return;
    }
    if (button == 0) for (Footer f : footer()) if (inside(x, y, f.x, height - 29, f.width, 20)) {
      if (f.enabled) f.action.run(); return;
    }
    previewClick(x, y, button);
  }
  @Override
  protected void mouseReleased(int x, int y, int button) {
    if (button >= 0) {
      if (Display.isActive() && !modal()) {
        if (button == 0 && slider != null) slide(x);
        finishGesture(x, y, button);
      } else stopGesture();
      if (button == 0) { slider = null; bar.release(); }
    }
    super.mouseReleased(x, y, button);
  }
  protected void renderControl(Setting s, int y, int mx, int my) {
    int w = controlWidth(s);
    if (inline.editing(s)) inline.render(this, controlX(), y, w, mx, my, controlsTop(), controlsBottom());
    else if (ColourScreen.accepts(s)) colourButton(s.value.getAsString(), controlX(), y, w, mx, my);
    else if (usesSlider(s)) slider(fit(display(s), w - 8), controlX(), y, w, mx, my, (s.value.getAsDouble() - s.min) / Math.max(.0001, s.max - s.min));
    else button(fit(display(s), w - 8), controlX(), y, w, 18, mx, my, true);
    if (resettable(s)) button("R", resetX(), y, 20, 18, mx, my, !s.value.equals(s.defaultValue));
  }
  public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    if (modal()) { mx = -1000; my = -1000; }
    if (!Display.isActive() || modal()) { slider = null; stopGesture(); bar.release(); inline.cancel(); }
    beforeRender(mx, my); inline.beginFrame();
    if (slider != null) { if (Mouse.isButtonDown(0)) slide(mx); else slider = null; }
    scroll = Math.max(0, Math.min(track().maximum(), bar.drag(track(), my, !modal() && Mouse.isButtonDown(0))));
    renderBackground(); drawCenteredTextWithShadow(textRenderer, title(), width / 2, 13, 0xffffff); renderToolbar(mx, my);
    fill(left(), controlsTop() - 2, right(), controlsBottom(), 0x80000000);
    List<ControlRow> rows = controlRows(); StickyHeader sticky = stickyHeader(rows);
    int rowsTop = sticky == null ? controlsTop() : Math.max(controlsTop(), sticky.y() + sticky.height());
    clip(left(), rowsTop, span() - 8, Math.max(0, controlsBottom() - rowsTop));
    int listMouseY = sticky != null && sticky.contains(my, controlsTop()) ? -1000 : my;
    String tip = "";
    for (int index = 0; index < rows.size(); index++) {
      ControlRow row = rows.get(index);
      int y = controlsTop() + row.y - (int)scroll;
      if (y >= controlsBottom()) break;
      if (y < controlsTop() || y + 18 > controlsBottom()) continue;
      if (row.setting == null) {
        if (sticky == null || index != sticky.index()) groupHeading(row, y);
        continue;
      }
      Setting s = row.setting;
      text(fit(label(s) + (s.restart ? " *" : ""), span() / 2 - 12), left() + 4, y + 6, s.changed() ? 0xffdd88 : 0xdddddd);
      renderControl(s, y, mx, listMouseY);
      if (inside(mx, listMouseY, left(), y, span() - 9, 23)) tip = inline.editing(s) ? inline.help()
          : s.description + (s.numeric() ? "\nRight-click to enter an exact value." : "") + (s.restart ? "\nRestart required." : "");
      if (resettable(s) && inside(mx, listMouseY, resetX(), y, 20, 18)) tip = "Reset to " + s.display(s.defaultValue);
    }
    if (sticky != null) {
      clip(left(), controlsTop(), span() - 8, controlsBottom() - controlsTop());
      groupHeading(rows.get(sticky.index()), sticky.y());
    }
    unclip(); bar.render(this, track()); renderPreview(mx, my, delta); inline.endFrame();
    text(fit(error.isEmpty() ? hint() : error, span()), left(), height - 43, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    for (Footer f : footer()) button(fit(f.label, f.width - 8), f.x, height - 29, f.width, 20, mx, my, f.enabled);
    if (!modal() && !tip.isBlank()) tooltip(tip, mx, my);
    inline.renderError(this);
    if (resetPrompt) {
      fill(0, 0, width, height, 0xc0000000);
      drawCenteredTextWithShadow(textRenderer, fit(resetQuestion(), width - 20), width / 2, height / 2 - 43, 0xffffff);
      for (ResetButton f : resetButtons()) button(fit(f.label, f.width - 8), f.x, f.y, f.width, 20, pointerX, pointerY, f.enabled);
    }
    exit.render(this, "Save changes to " + title().toLowerCase(Locale.ROOT) + "?", pointerX, pointerY);
  }
}
