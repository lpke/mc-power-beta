package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Select, drag and precisely edit the same anchored layouts used during gameplay. */
public final class HudEditorScreen extends DraftEditorScreen {
  private final HudPreview preview;
  private int tab, scenario;
  private String selected = "combined";
  private boolean snapping = true, selecting;
  private final EditorSplit divider = new EditorSplit();
  private int popupIndex;
  private HudPreview.Element dragging;
  private double pressX, pressY;
  private int startX, startY;

  public HudEditorScreen(PowerOptionsScreen parent, String settingId) {
    super(parent, new SettingsDraft(parent.session().settings(), HudEditorSettings::contains));
    preview = new HudPreview(draft);
    if (preview.mode() != 1) selected = "native.0";
    if (settingId.startsWith("infoHud.")) {
      int cluster = settingId.startsWith("infoHud.clusters.") ? Character.digit(settingId.charAt("infoHud.clusters.".length()), 10) : preview.integer("infoHud.selectedCluster");
      select("cluster." + cluster);
    } else if (settingId.startsWith("activeTweaks.")) select("tweaks");
    else {
      Setting setting = draft.get(settingId);
      if (setting.group.equals("Version label")) select("version");
      else if (setting.group.equals("Debug (F3) info")) select("debug");
      else if (preview.mode() == 2) {
        int index = 0;
        for (int i = 0; i < HudPreview.PARTS.length; i++) if (settingId.contains(HudPreview.PARTS[i] + "_POSITION_CONFIG")) index = i;
        select("native." + index);
      }
    }
  }
  protected String title() { return "HUD editor"; }
  protected int controlsTop() { return 80; }
  protected int controlsBottom() { return divider.bottom(controlsTop(), previewBottom()); }
  protected void beforeRender(int x, int y) {
    if (divider.dragging()) {
      if (Mouse.isButtonDown(0) && !modal()) divider.move(y, controlsTop(), previewBottom()); else divider.stop();
    }
  }
  protected String label(Setting s) { return s.id.equals(HudEditorSettings.ENTRY) ? "Positioning" : super.label(s); }
  protected List<Setting> controls() {
    if (tab == 0) {
      String prefix = preview.mode() == 1 ? HudPreview.SIMPLE : HudPreview.nativePrefix(nativeIndex());
      List<Setting> values = new ArrayList<>(); values.add(draft.get(HudEditorSettings.ENTRY));
      draft.settings().stream().filter(s -> s.id.equals(HudPreview.NATIVE + "putStatusBarIconsBelowHotbar")
          || s.id.equals(HudPreview.NATIVE + "putOverlayMessagesBelowHotbar")
          || preview.mode() != 0 && s.id.startsWith(prefix)).forEach(values::add);
      return values;
    }
    if (tab == 1) {
      int cluster = selected.startsWith("cluster.") ? Integer.parseInt(selected.substring(8)) : 0;
      return draft.settings().stream().filter(s -> s.id.equals("infoHud.enabled") || s.id.equals("infoHud.selectedCluster")
          || s.id.startsWith("infoHud.clusters." + cluster + ".")).toList();
    }
    if (tab == 2) return draft.settings().stream().filter(s -> s.id.startsWith("activeTweaks.") && !s.id.endsWith("Messages")).toList();
    return draft.settings().stream().filter(s -> s.group.equals(tab == 3 ? "Version label" : "Debug (F3) info")).toList();
  }
  private int nativeIndex() { return selected.startsWith("native.") ? Integer.parseInt(selected.substring(7)) : 0; }
  private List<String> selectionIds() {
    List<String> ids = new ArrayList<>();
    if (preview.mode() == 1) ids.add("combined"); else for (int i = 0; i < 5; i++) ids.add("native." + i);
    for (int i = 0; i < 4; i++) ids.add("cluster." + i);
    ids.addAll(List.of("tweaks", "version", "debug")); return ids;
  }
  private String selectedLabel() {
    if (selected.equals("combined")) return "Combined HUD";
    if (selected.startsWith("native.")) return HudPreview.NAMES[nativeIndex()];
    if (selected.startsWith("cluster.")) return "Cluster " + (Integer.parseInt(selected.substring(8)) + 1);
    return selected.equals("tweaks") ? "Active tweaks" : selected.equals("version") ? "Version label" : "F3 information";
  }
  private void select(String id) {
    selected = id; selecting = false; stopGesture(); controlsChanged();
    if (id.startsWith("cluster.")) {
      tab = 1; draft.get("infoHud.selectedCluster").value = new JsonPrimitive(Integer.parseInt(id.substring(8)));
    } else if (id.equals("tweaks")) tab = 2;
    else if (id.equals("version")) tab = 3;
    else if (id.equals("debug")) { tab = 4; scenario = 2; }
    else { tab = 0; if (id.equals("native.3")) scenario = 1; }
  }
  protected void afterEdit() {
    preview.rebuild();
    if (tab == 1) selected = "cluster." + preview.integer("infoHud.selectedCluster");
    if (tab == 0) {
      if (preview.mode() == 1) selected = "combined";
      else if (!selected.startsWith("native.")) selected = "native.0";
    }
  }
  private int tabWidth() { return (span() - 16) / 5; }
  private int selectorWidth() { return Math.max(72, span() - 154); }
  protected void renderToolbar(int x, int y) {
    String[] tabs = {"Layout", "Info", "Tweaks", "Version", "F3"}; int w = tabWidth();
    for (int i = 0; i < tabs.length; i++) tabButton(tabs[i], left() + i * (w + 4), 30, w, x, y, tab == i);
    int top = 55, sw = selectorWidth();
    button("", left(), top, sw, 18, x, y, true);
    text(fit(selectedLabel(), sw - 24), left() + 5, top + 5, 0xe0e0e0);
    chevron(left() + sw - 13, top + 7, selecting, 0xffdddddd);
    button(new String[]{"Normal", "Underwater", "F3"}[scenario], left() + sw + 4, top, 92, 18, x, y, true);
    button(snapping ? "Snap: On" : "Snap: Off", left() + sw + 100, top, 54, 18, x, y, true);
  }
  protected boolean toolbarClick(int x, int y, int button) {
    if (button != 0 && button != 1) return false;
    int w = tabWidth();
    for (int i = 0; i < 5; i++) if (inside(x, y, left() + i * (w + 4), 30, w, 18)) {
      select(i == 0 ? preview.mode() == 1 ? "combined" : "native.0" : i == 1 ? "cluster." + preview.integer("infoHud.selectedCluster") : i == 2 ? "tweaks" : i == 3 ? "version" : "debug");
      return true;
    }
    if (button == 0 && inside(x, y, left(), controlsBottom() + 1, span(), 10)) {
      stopGesture(); divider.start(y, controlsTop(), previewBottom()); return true;
    }
    int top = 55, sw = selectorWidth();
    if (inside(x, y, left(), top, sw, 18)) {
      selecting = !selecting; popupIndex = Math.max(0, selectionIds().indexOf(selected)); stopGesture(); controlsChanged(); return true;
    }
    if (inside(x, y, left() + sw + 4, top, 92, 18)) { scenario = Math.floorMod(scenario + (button == 1 ? -1 : 1), 3); return true; }
    if (inside(x, y, left() + sw + 100, top, 54, 18)) { snapping = !snapping; return true; }
    return false;
  }
  private double scale() { return Math.min((span() - 4d) / width, Math.max(1, previewBottom() - previewTop() - 4d) / height); }
  private double canvasX() { return width / 2d - width * scale() / 2; }
  private double canvasY() { return previewTop() + (previewBottom() - previewTop() - height * scale()) / 2; }
  private double virtualX(int x) { return (x - canvasX()) / scale(); }
  private double virtualY(int y) { return (y - canvasY()) / scale(); }
  protected void previewClick(int x, int y, int button) {
    if (button != 0 || !inside(x, y, left(), previewTop(), span(), previewBottom() - previewTop())) return;
    List<HudPreview.Element> elements = preview.elements();
    for (int i = elements.size() - 1; i >= 0; i--) {
      var element = elements.get(i);
      if (!element.contains(virtualX(x), virtualY(y))) continue;
      select(element.id());
      if (element.offsetX() != null || element.offsetY() != null) {
        draft.checkpoint(); dragging = element; pressX = virtualX(x); pressY = virtualY(y);
        startX = element.offsetX() == null ? 0 : preview.integer(element.offsetX());
        startY = element.offsetY() == null ? 0 : preview.integer(element.offsetY());
      }
      return;
    }
  }
  protected void finishGesture(int x, int y, int button) {
    if (button != 0) return;
    if (divider.dragging()) divider.move(y, controlsTop(), previewBottom());
    if (dragging != null) move(x, y);
    stopGesture();
  }
  protected void stopGesture() { dragging = null; divider.stop(); }
  protected void mouseClicked(int x, int y, int button) {
    if (selecting && !modal()) {
      int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = span() / columns;
      int top = 76;
      if (button == 0 && inside(x, y, left(), top, span(), rows * 20)) {
        int index = (x - left()) / cellWidth * rows + (y - top) / 20;
        if (index < selectionIds().size()) select(selectionIds().get(index));
      }
      selecting = false; return;
    }
    super.mouseClicked(x, y, button);
  }
  protected void keyPressed(char c, int key) {
    if (selecting && !modal()) {
      if (key == Keyboard.KEY_ESCAPE) selecting = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        popupIndex = Math.floorMod(popupIndex + (key == Keyboard.KEY_UP ? -1 : 1), selectionIds().size());
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) select(selectionIds().get(popupIndex));
      return;
    }
    super.keyPressed(c, key);
  }
  private int selectorRows() { return Math.max(1, Math.min(selectionIds().size(), (height - 120) / 20)); }
  public void render(int x, int y, float delta) {
    super.render(selecting ? -1000 : x, selecting ? -1000 : y, delta);
    if (!selecting || modal()) return;
    int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = span() / columns;
    int top = 76; List<String> ids = selectionIds();
    fill(left() - 2, top - 2, right() + 2, top + rows * 20 + 2, 0xff777777);
    fill(left() - 1, top - 1, right() + 1, top + rows * 20 + 1, 0xff181d23);
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i), label = id.startsWith("cluster.") ? "Cluster " + (Integer.parseInt(id.substring(8)) + 1)
          : id.startsWith("native.") ? HudPreview.NAMES[Integer.parseInt(id.substring(7))]
          : id.equals("combined") ? "Combined HUD" : id.equals("tweaks") ? "Active tweaks" : id.equals("version") ? "Version label" : "F3 information";
      int px = left() + i / rows * cellWidth, py = top + i % rows * 20;
      boolean hover = inside(x, y, px, py, cellWidth, 20);
      if (hover || i == popupIndex) fill(px, py, px + cellWidth - 1, py + 19, 0xff344452);
      if (id.equals(selected)) fill(px + 2, py + 3, px + 4, py + 16, 0xff88ccee);
      text(fit(label, cellWidth - 17), px + 9, py + 6, hover ? 0xffffaa : id.equals(selected) ? 0xffffff : 0xcccccc);
    }
  }

  private int snap(double coordinate, double size, int extent) {
    if (snapping) for (double target : new double[]{2, (extent - size) / 2, extent - size - 2})
      if (Math.abs(coordinate - target) <= 4) return (int)Math.round(target);
    return (int)Math.round(coordinate);
  }
  private void move(int x, int y) {
    if (dragging == null) return;
    double nextX = Math.max(0, Math.min(width - dragging.width(), dragging.x() + virtualX(x) - pressX));
    double nextY = Math.max(0, Math.min(height - dragging.height(), dragging.y() + virtualY(y) - pressY));
    if (dragging.offsetX() != null) setOffset(dragging.offsetX(), startX + snap(nextX, dragging.width(), width) - dragging.x());
    if (dragging.offsetY() != null) setOffset(dragging.offsetY(), startY + snap(nextY, dragging.height(), height) - dragging.y());
  }
  private void setOffset(String id, double value) {
    Setting s = draft.get(id); s.value = new JsonPrimitive((int)Math.max(s.min, Math.min(s.max, Math.round(value))));
    if (id.startsWith("infoHud.") || id.startsWith("activeTweaks.")) preview.rebuild();
  }
  protected boolean editorKey(char c, int key) {
    int dx = key == Keyboard.KEY_LEFT ? -1 : key == Keyboard.KEY_RIGHT ? 1 : 0;
    int dy = key == Keyboard.KEY_UP ? -1 : key == Keyboard.KEY_DOWN ? 1 : 0;
    if (dx == 0 && dy == 0) return false;
    var element = preview.elements().stream().filter(e -> e.id().equals(selected)).findFirst().orElse(null);
    if (element != null) {
      draft.checkpoint();
      int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 10 : 1;
      if (element.offsetX() != null) setOffset(element.offsetX(), preview.integer(element.offsetX()) + dx * step);
      if (element.offsetY() != null) setOffset(element.offsetY(), preview.integer(element.offsetY()) + dy * step);
    }
    return true;
  }
  protected String hint() { return "Drag the divider to resize. Select an element above; drag it to move. Arrows fine-tune."; }
  protected void renderPreview(int x, int y, float delta) {
    int line = controlsBottom() + 5;
    boolean hover = inside(x, y, left(), controlsBottom() + 1, span(), 10);
    fill(left(), line, right(), line + 1, divider.dragging() || hover ? 0xffaabbcc : 0xff68737c);
    fill(width / 2 - 12, line - 2, width / 2 + 12, line + 3, 0xff343e48);
    fill(width / 2 - 8, line - 1, width / 2 + 8, line, 0xffaabbcc);
    fill(width / 2 - 8, line + 1, width / 2 + 8, line + 2, 0xffaabbcc);
    if (dragging != null) { if (!modal() && Mouse.isButtonDown(0)) move(x, y); else stopGesture(); }
    fill(left(), previewTop(), right(), previewBottom(), 0xb010161d);
    clip(left(), previewTop(), span(), Math.max(0, previewBottom() - previewTop()));
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glTranslated(canvasX(), canvasY(), 0); GL11.glScaled(scale(), scale(), 1);
      fill(0, 0, width, height, 0xff26323c);
      if (dragging != null && snapping) {
        fill(width / 2, 0, width / 2 + 1, height, 0x5566bbff);
        fill(0, height / 2, width, height / 2 + 1, 0x5566bbff);
      }
      preview.draw(this, minecraft, width, height, scenario, selected);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); unclip(); }
  }
}
