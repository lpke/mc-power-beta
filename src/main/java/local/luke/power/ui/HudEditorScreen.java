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
  private int scenario;
  private String selected = "layout";
  private boolean snapping = true, selecting;
  private final EditorSplit divider = new EditorSplit();
  private int popupIndex;
  private HudPreview.Element dragging;
  private double pressX, pressY;
  private int startX, startY;

  public HudEditorScreen(PowerOptionsScreen parent, String settingId) {
    super(parent, new SettingsDraft(parent.session().settings(), HudEditorSettings::contains));
    preview = new HudPreview(draft);
    if (settingId.startsWith("infoHud.")) {
      int cluster = settingId.startsWith("infoHud.clusters.") ? Character.digit(settingId.charAt("infoHud.clusters.".length()), 10) : preview.integer("infoHud.selectedCluster");
      select("cluster." + cluster);
    } else if (settingId.startsWith("activeTweaks.")) select("tweaks");
    else {
      Setting setting = draft.get(settingId);
      if (setting.group.equals("Version label")) select("version");
      else if (setting.group.equals("Debug (F3) info")) select("debug");
      else if (settingId.startsWith(HudPreview.SIMPLE) || settingId.contains("_POSITION_CONFIG.")) {
        int index = 0;
        for (int i = 0; i < HudPreview.PARTS.length; i++) if (settingId.contains(HudPreview.PARTS[i] + "_POSITION_CONFIG")) index = i;
        select(preview.mode() == 1 ? "combined" : "native." + index);
      }
    }
  }
  protected String title() { return "HUD editor"; }
  protected int controlsBottom() { return divider.bottom(controlsTop(), previewBottom()); }
  protected void beforeRender(int x, int y) {
    if (divider.dragging()) {
      if (Mouse.isButtonDown(0) && !modal()) divider.move(y, controlsTop(), previewBottom()); else divider.stop();
    }
  }
  protected String label(Setting s) {
    if (s.id.equals(HudEditorSettings.ENTRY)) return "HUD positioning";
    if (s.id.equals("infoHud.enabled")) return "Show info HUD";
    if (s.id.equals("power_compat:config.overlayAdditionsYOffset")) return "Extra info vertical offset";
    if (s.id.equals("entityculling:config.f3InfoYOffset") || s.id.endsWith("overlayForMusicInDebugYOffset")) return "Vertical offset";
    if (s.id.startsWith(HudPreview.NATIVE)) return switch (s.id.substring(s.id.lastIndexOf('.') + 1)) {
      case "horizontalPosition" -> "Horizontal anchor";
      case "verticalPosition" -> "Vertical anchor";
      case "horizontalPositionOffset" -> "Horizontal offset";
      case "verticalPositionOffset" -> "Vertical offset";
      case "enableVisibility" -> selected.equals("combined") ? "Show combined HUD" : "Show " + selectedLabel().toLowerCase(Locale.ROOT);
      case "putStatusBarIconsBelowHotbar" -> "Status icons below hotbar";
      case "putOverlayMessagesBelowHotbar" -> "Messages below hotbar";
      default -> super.label(s);
    };
    return super.label(s);
  }
  protected String controlGroup(Setting s) { return HudEditorSections.group(s); }
  protected List<Setting> controls() {
    if (selected.equals("layout")) return HudEditorSections.order(List.of(draft.get(HudEditorSettings.ENTRY),
        draft.get(HudPreview.NATIVE + "putStatusBarIconsBelowHotbar"), draft.get(HudPreview.NATIVE + "putOverlayMessagesBelowHotbar")));
    if (selected.equals("combined") || selected.startsWith("native.")) {
      String prefix = preview.mode() == 1 ? HudPreview.SIMPLE : HudPreview.nativePrefix(nativeIndex());
      if (preview.mode() == 0) return List.of(draft.get(HudEditorSettings.ENTRY));
      return HudEditorSections.order(draft.settings().stream().filter(s -> s.id.startsWith(prefix)).toList());
    }
    if (selected.startsWith("cluster.")) {
      int cluster = Integer.parseInt(selected.substring(8));
      return HudEditorSections.order(draft.settings().stream().filter(s -> s.id.equals("infoHud.enabled")
          || s.id.startsWith("infoHud.clusters." + cluster + ".")).toList());
    }
    if (selected.equals("tweaks")) return HudEditorSections.order(draft.settings().stream()
        .filter(s -> s.id.startsWith("activeTweaks.") && !s.id.endsWith("Messages")).toList());
    return HudEditorSections.order(draft.settings().stream().filter(s -> s.group.equals(selected.equals("version") ? "Version label" : "Debug (F3) info")).toList());
  }
  protected boolean offersPositionReset() { return true; }
  protected List<Setting> positionSettings() { return controls().stream().filter(HudEditorSections::position).toList(); }
  protected String resetQuestion() { return "Reset " + selectedLabel().toLowerCase(Locale.ROOT) + "?"; }
  private int nativeIndex() { return selected.startsWith("native.") ? Integer.parseInt(selected.substring(7)) : 0; }
  private List<String> selectionIds() {
    List<String> ids = new ArrayList<>(); ids.add("layout");
    if (preview.mode() == 1) ids.add("combined"); else for (int i = 0; i < 5; i++) ids.add("native." + i);
    for (int i = 0; i < 4; i++) ids.add("cluster." + i);
    ids.addAll(List.of("tweaks", "version", "debug")); return ids;
  }
  private String selectedLabel() { return selectionLabel(selected); }
  private String selectionLabel(String id) {
    if (id.equals("layout")) return "General layout";
    if (id.equals("combined")) return "Combined HUD";
    if (id.startsWith("native.")) return HudPreview.NAMES[Integer.parseInt(id.substring(7))];
    if (id.startsWith("cluster.")) return "Info cluster " + (Integer.parseInt(id.substring(8)) + 1);
    return id.equals("tweaks") ? "Active tweaks" : id.equals("version") ? "Version label" : "F3 information";
  }
  private void select(String id) {
    boolean changed = !selected.equals(id);
    selected = id; selecting = false; stopGesture();
    if (changed) controlsChanged();
    if (id.startsWith("cluster.")) {
      draft.get("infoHud.selectedCluster").value = new JsonPrimitive(Integer.parseInt(id.substring(8)));
    } else if (id.equals("debug")) scenario = 2;
    else if (id.equals("native.3")) scenario = 1;
  }
  protected void afterEdit() {
    preview.rebuild();
    if (selected.startsWith("cluster.")) selected = "cluster." + preview.integer("infoHud.selectedCluster");
    if (selected.equals("combined") || selected.startsWith("native.")) {
      if (preview.mode() == 1) selected = "combined";
      else if (!selected.startsWith("native.")) selected = "native.0";
    }
  }
  private int selectorWidth() { return Math.max(72, span() - 154); }
  protected void renderToolbar(int x, int y) {
    int top = 30, sw = selectorWidth();
    button("", left(), top, sw, 18, x, y, true);
    text(fit(selectedLabel(), sw - 24), left() + 5, top + 5, 0xe0e0e0);
    chevron(left() + sw - 13, top + 7, selecting, 0xffdddddd);
    button(new String[]{"Normal", "Underwater", "F3"}[scenario], left() + sw + 4, top, 92, 18, x, y, true);
    button(snapping ? "Snap: On" : "Snap: Off", left() + sw + 100, top, 54, 18, x, y, true);
  }
  protected boolean toolbarClick(int x, int y, int button) {
    if (button != 0 && button != 1) return false;
    if (button == 0 && inside(x, y, left(), controlsBottom() + 1, span(), 10)) {
      stopGesture(); divider.start(y, controlsTop(), previewBottom()); return true;
    }
    int top = 30, sw = selectorWidth();
    if (inside(x, y, left(), top, sw, 18)) {
      selecting = !selecting; popupIndex = Math.max(0, selectionIds().indexOf(selected)); stopGesture(); return true;
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
      if (!element.id().equals(selected)) { select(element.id()); return; }
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
      int top = 51;
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
      else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT)
        popupIndex = Math.floorMod(popupIndex + (key == Keyboard.KEY_LEFT ? -selectorRows() : selectorRows()), selectionIds().size());
      return;
    }
    super.keyPressed(c, key);
  }
  private int selectorRows() { return Math.max(1, Math.min(selectionIds().size(), (height - 96) / 20)); }
  public void render(int x, int y, float delta) {
    super.render(selecting ? -1000 : x, selecting ? -1000 : y, delta);
    if (!selecting || modal()) return;
    int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = span() / columns;
    int top = 51; List<String> ids = selectionIds();
    fill(left() - 2, top - 2, right() + 2, top + rows * 20 + 2, 0xff777777);
    fill(left() - 1, top - 1, right() + 1, top + rows * 20 + 1, 0xff181818);
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i), label = selectionLabel(id);
      int px = left() + i / rows * cellWidth, py = top + i % rows * 20;
      boolean hover = inside(x, y, px, py, cellWidth, 20);
      if (hover || i == popupIndex) fill(px, py, px + cellWidth - 1, py + 19, 0xff3c3c3c);
      if (id.equals(selected)) fill(px + 2, py + 3, px + 4, py + 16, 0xffcccccc);
      text(fit(label, cellWidth - 17), px + 9, py + 6, hover || id.equals(selected) ? 0xffffff : 0xcccccc);
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
  protected String hint() { return ""; }
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
