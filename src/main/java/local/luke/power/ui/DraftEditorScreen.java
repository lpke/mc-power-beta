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
  protected void reset() { controls().forEach(Setting::reset); }
  protected void resetFinished() {}
  protected void afterEdit() {}
  protected void valueChanged(Setting s) { afterEdit(); }
  protected String hint() { return "Click an element to edit it. Drag to move. Arrow keys fine-tune."; }
  protected String resetLabel() { return "Reset..."; }
  protected String resetQuestion() { return "Reset the selected settings?"; }
  protected String extraFooterLabel() { return ""; }
  protected void extraFooterAction() {}
  protected void controlsChanged() { scroll = 0; bar.release(); slider = null; inline.cancel(); }
  protected boolean modal() { return exit.active() || resetPrompt; }
  protected int controlX() { return left() + span() / 2; }
  protected int controlWidth() { return span() / 2 - 10; }
  private ScrollBar.Track track() {
    return new ScrollBar.Track(right() - 4, controlsTop(), Math.max(0, controlsBottom() - controlsTop()), controls().size() * 23, scroll);
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
      openInline(s, () -> valueChanged(s), controlX(), y, controlWidth());
    else minecraft.setScreen(new ValueScreen(s, this, () -> valueChanged(s)));
  }
  private void slide(int x) {
    if (slider == null) return;
    slider.slide((x - controlX() - 4d) / Math.max(1, controlWidth() - 8)); valueChanged(slider);
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
  protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent))) return;
    if (resetPrompt) {
      if (button == 0 && inside(x, y, width / 2 - 82, height / 2 + 8, 80, 20)) {
        draft.checkpoint(); reset(); afterEdit(); resetFinished(); resetPrompt = false;
      } else if (button == 0 && inside(x, y, width / 2 + 2, height / 2 + 8, 80, 20)) resetPrompt = false;
      return;
    }
    if (inline.press(x, y, button)) return;
    slider = null; bar.release();
    if (toolbarClick(x, y, button)) return;
    if (button == 0 && bar.press(track(), x, y)) return;
    if ((button == 0 || button == 1) && inside(x, y, left(), controlsTop(), span() - 9, controlsBottom() - controlsTop())) {
      int row = (int)((y - controlsTop() + scroll) / 23);
      List<Setting> values = controls(); int rowY = controlsTop() + row * 23 - (int)scroll;
      if (row < values.size() && rowY >= controlsTop() && rowY + 18 <= controlsBottom() && y < rowY + 18 && x >= controlX()) {
        Setting s = values.get(row);
        if (button == 0 && usesSlider(s)) { draft.checkpoint(); slider = s; slide(x); }
        else edit(s, button == 1 ? -1 : 1, rowY);
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
    if (inline.editing(s)) inline.render(this, controlX(), y, controlWidth(), mx, my, controlsTop(), controlsBottom());
    else if (ColourScreen.accepts(s)) colourButton(s.value.getAsString(), controlX(), y, controlWidth(), mx, my);
    else if (usesSlider(s)) slider(fit(display(s), controlWidth() - 8), controlX(), y, controlWidth(), mx, my, (s.value.getAsDouble() - s.min) / Math.max(.0001, s.max - s.min));
    else button(fit(display(s), controlWidth() - 8), controlX(), y, controlWidth(), 18, mx, my, true);
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
    clip(left(), controlsTop(), span() - 8, Math.max(0, controlsBottom() - controlsTop()));
    List<Setting> values = controls(); String tip = "";
    for (int i = (int)scroll / 23; i < values.size(); i++) {
      int y = controlsTop() + i * 23 - (int)scroll;
      if (y >= controlsBottom()) break;
      if (y < controlsTop() || y + 18 > controlsBottom()) continue;
      Setting s = values.get(i);
      text(fit(label(s) + (s.restart ? " *" : ""), span() / 2 - 12), left() + 4, y + 6, s.changed() ? 0xffdd88 : 0xdddddd);
      renderControl(s, y, mx, my);
      if (inside(mx, my, left(), y, span() - 9, 23)) tip = inline.editing(s) ? inline.help()
          : s.description + (s.numeric() ? "\nRight-click to enter an exact value." : "") + (s.restart ? "\nRestart required." : "");
    }
    unclip(); bar.render(this, track()); renderPreview(mx, my, delta); inline.endFrame();
    text(fit(error.isEmpty() ? hint() : error, span()), left(), height - 43, error.isEmpty() ? 0xaaaaaa : 0xff8888);
    for (Footer f : footer()) button(fit(f.label, f.width - 8), f.x, height - 29, f.width, 20, mx, my, f.enabled);
    if (!modal() && !tip.isBlank()) tooltip(tip, mx, my);
    inline.renderError(this);
    if (resetPrompt) {
      fill(0, 0, width, height, 0xc0000000);
      drawCenteredTextWithShadow(textRenderer, fit(resetQuestion(), width - 20), width / 2, height / 2 - 16, 0xffffff);
      button("Reset", width / 2 - 82, height / 2 + 8, 80, 20, pointerX, pointerY, true);
      button("Cancel", width / 2 + 2, height / 2 + 8, 80, 20, pointerX, pointerY, true);
    }
    exit.render(this, "Save changes to " + title().toLowerCase(Locale.ROOT) + "?", pointerX, pointerY);
  }
}
