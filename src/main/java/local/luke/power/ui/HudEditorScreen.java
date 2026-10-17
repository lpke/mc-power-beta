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
  private int lastNativeIndex;
  private String selected = "layout";
  private boolean snapping = true, selecting, background = true;
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
      if (!HudEditorSettings.view(setting).isEmpty()) select(HudEditorSettings.view(setting));
      else if (HudEditorSettings.itemNames(setting)) select(preview.mode() == 1 ? "combined" : "native.4");
      else if (setting.group.equals("Version label")) select("version");
      else if (setting.group.equals("Debug (F3) info")) select("debug");
      else if (settingId.startsWith(HudPreview.SIMPLE) || settingId.contains("_POSITION_CONFIG.")) {
        int index = 0;
        for (int i = 0; i < HudPreview.PARTS.length; i++) if (settingId.contains(HudPreview.PARTS[i] + "_POSITION_CONFIG")) index = i;
        select(preview.mode() == 1 ? "combined" : "native." + index);
      }
    }
  }
  protected String title() { return "HUD editor"; }
  public void init() { super.init(); HudPreviewBackground.prepare(minecraft); }
  private HudEditorLayout geometry() {
    return HudEditorLayout.at(width, height, HudEditorLayout.horizontal(width, height)
        ? divider.sideWidth() : divider.bottom(55, height - 53));
  }
  protected int span() { return width - 20; }
  protected int controlsLeft() { return geometry().controlsLeft(); }
  protected int controlsSpan() { return geometry().controlsWidth(); }
  protected int controlsBottom() { return geometry().controlsBottom(); }
  protected int previewLeft() { return geometry().previewLeft(); }
  protected int previewSpan() { return geometry().previewWidth(); }
  protected int previewTop() { return geometry().previewTop(); }
  protected boolean resettable(Setting s) { return true; }
  private void moveDivider(int x, int y) {
    if (divider.sideways()) divider.moveSide(x, left(), width);
    else divider.move(y, controlsTop(), previewBottom());
  }
  protected void beforeRender(int x, int y) {
    if (divider.dragging()) {
      if (Mouse.isButtonDown(0) && !modal() && divider.sideways() == geometry().horizontal()) moveDivider(x, y); else divider.stop();
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
  protected String groupKey(String group) { return selected + "/" + group; }
  protected List<Setting> controls() {
    if (selected.equals("layout")) return HudEditorSections.order(List.of(draft.get(HudEditorSettings.ENTRY),
        draft.get(HudPreview.NATIVE + "putStatusBarIconsBelowHotbar"), draft.get(HudPreview.NATIVE + "putOverlayMessagesBelowHotbar"),
        draft.get(HudEditorSettings.HUD + "disableCrosshair"), draft.get(HudEditorSettings.HUD + "drawXboxXAndYButtons"), draft.get(HudEditorSettings.TOAST)));
    if (selected.equals("combined") || selected.startsWith("native.")) {
      String prefix = preview.mode() == 1 ? HudPreview.SIMPLE : HudPreview.nativePrefix(nativeIndex());
      List<Setting> values = new ArrayList<>(preview.mode() == 0 ? List.of(draft.get(HudEditorSettings.ENTRY))
          : draft.settings().stream().filter(s -> s.id.startsWith(prefix)).toList());
      if (selected.equals("combined") || nativeIndex() == 4) {
        values.add(draft.get(HudEditorSettings.ITEM_NAMES)); values.add(draft.get(HudEditorSettings.ITEM_FADE));
      }
      return HudEditorSections.order(values);
    }
    if (selected.startsWith("cluster.")) {
      int cluster = Integer.parseInt(selected.substring(8));
      return HudEditorSections.order(draft.settings().stream().filter(s -> s.id.equals("infoHud.enabled")
          || s.id.startsWith("infoHud.clusters." + cluster + ".")).toList());
    }
    if (selected.equals("tweaks")) return HudEditorSections.order(draft.settings().stream()
        .filter(s -> s.id.startsWith("activeTweaks.") && !s.id.endsWith("Messages")).toList());
    if (selected.equals("chat")) return HudEditorSections.order(draft.settings().stream()
        .filter(s -> HudEditorSettings.view(s).equals(selected)).toList());
    return HudEditorSections.order(draft.settings().stream().filter(s -> s.group.equals(selected.equals("version") ? "Version label" : "Debug (F3) info")).toList());
  }
  protected boolean offersPositionReset() { return true; }
  protected List<Setting> positionSettings() { return controls().stream().filter(HudEditorSections::position).toList(); }
  protected String resetSubject() { return selectedLabel(); }
  protected List<Setting> allResetSettings() { return draft.settings().stream().filter(s -> !s.id.equals("infoHud.selectedCluster") && !s.id.equals(HudEditorSettings.COLOURS)).toList(); }
  private int nativeIndex() { return selected.startsWith("native.") ? Integer.parseInt(selected.substring(7)) : 0; }
  private List<String> selectionIds() {
    List<String> ids = new ArrayList<>(); ids.add("layout");
    if (preview.mode() == 1) ids.add("combined"); else for (int i = 0; i < 5; i++) ids.add("native." + i);
    for (int i = 0; i < 4; i++) ids.add("cluster." + i);
    ids.addAll(List.of("chat", "tweaks", "version", "debug")); return ids;
  }
  private String selectedLabel() { return selectionLabel(selected); }
  private String selectionLabel(String id) {
    if (id.equals("layout")) return "General";
    if (id.equals("combined")) return "Combined HUD";
    if (id.startsWith("native.")) return HudPreview.NAMES[Integer.parseInt(id.substring(7))];
    if (id.startsWith("cluster.")) return "Info cluster " + (Integer.parseInt(id.substring(8)) + 1);
    if (id.equals("chat")) return "Chat";
    return id.equals("tweaks") ? "Active tweaks" : id.equals("version") ? "Version label" : "F3 information";
  }
  private void select(String id) {
    boolean changed = !selected.equals(id);
    selected = id; selecting = false; stopGesture();
    if (id.startsWith("native.")) lastNativeIndex = nativeIndex();
    if (changed) controlsChanged();
    if (id.startsWith("cluster.")) {
      draft.get("infoHud.selectedCluster").value = new JsonPrimitive(Integer.parseInt(id.substring(8)));
    } else if (id.equals("debug")) scenario = 2;
    else if (Set.of("layout", "native.3", "chat", "version").contains(id)) scenario = 0;
  }
  protected void afterEdit() {
    preview.rebuild();
    if (selected.startsWith("cluster.")) selected = "cluster." + preview.integer("infoHud.selectedCluster");
    if (selected.equals("combined") || selected.startsWith("native.")) {
      if (selected.startsWith("native.")) lastNativeIndex = nativeIndex();
      if (preview.mode() == 1) selected = "combined";
      else if (!selected.startsWith("native.")) selected = "native." + lastNativeIndex;
    }
  }
  private int selectorWidth() { return geometry().horizontal() ? controlsSpan() : Math.max(72, span() - 186); }
  private int previewToolbarX() { return geometry().horizontal() ? previewLeft() : left() + selectorWidth() + 4; }
  private int selectorPopupWidth() { return geometry().horizontal() ? controlsSpan() : span(); }
  protected String hoverHelp(int x, int y) {
    int sw = selectorWidth(), px = previewToolbarX();
    if (inside(x, y, left(), 30, sw, 18)) return selectedLabel();
    if (inside(x, y, px, 30, 60, 18)) return "Switch between the normal HUD and F3 preview.";
    if (inside(x, y, px + 64, 30, 60, 18)) return "Snap dragged elements to the screen edges and centre.";
    if (inside(x, y, px + 128, 30, 54, 18)) return "Switch between a forest scene and a solid preview background.";
    return "";
  }
  protected void renderToolbar(int x, int y) {
    int top = 30, sw = selectorWidth(), px = previewToolbarX();
    button("", left(), top, sw, 18, x, y, true);
    text(fit(selectedLabel(), sw - 24), left() + 5, top + 5, 0xe0e0e0);
    chevron(left() + sw - 13, top + 7, selecting, 0xffdddddd);
    button(scenario == 2 ? "F3" : "Normal", px, top, 60, 18, x, y, true);
    button(snapping ? "Snap: On" : "Snap: Off", px + 64, top, 60, 18, x, y, true);
    button(background ? "Bg: On" : "Bg: Off", px + 128, top, 54, 18, x, y, true);
  }
  private boolean overDivider(int x, int y) {
    var g = geometry();
    return g.horizontal() ? inside(x, y, controlsRight() + 1, controlsTop(), 10, controlsBottom() - controlsTop())
        : inside(x, y, left(), controlsBottom() + 1, span(), 10);
  }
  protected boolean toolbarClick(int x, int y, int button) {
    if (button != 0 && button != 1) return false;
    if (button == 0 && overDivider(x, y)) {
      stopGesture();
      if (geometry().horizontal()) divider.startSide(x, left(), width);
      else divider.start(y, controlsTop(), previewBottom());
      return true;
    }
    int top = 30, sw = selectorWidth(), px = previewToolbarX();
    if (inside(x, y, left(), top, sw, 18)) {
      selecting = !selecting; popupIndex = Math.max(0, selectionIds().indexOf(selected)); stopGesture(); return true;
    }
    if (inside(x, y, px, top, 60, 18)) { scenario = scenario == 2 ? 0 : 2; return true; }
    if (inside(x, y, px + 64, top, 60, 18)) { snapping = !snapping; return true; }
    if (inside(x, y, px + 128, top, 54, 18)) { background = !background; return true; }
    return false;
  }
  private double scale() { return geometry().scale(width, height); }
  private double canvasX() { return geometry().canvasX(width, height); }
  private double canvasY() { return geometry().canvasY(width, height); }
  private double virtualX(int x) { return (x - canvasX()) / scale(); }
  private double virtualY(int y) { return (y - canvasY()) / scale(); }
  protected void previewClick(int x, int y, int button) {
    if (button != 0 || !inside(x, y, previewLeft(), previewTop(), previewSpan(), previewBottom() - previewTop())) return;
    List<HudPreview.Element> elements = preview.elements();
    for (int pass = 0; pass < 3; pass++) for (int i = elements.size() - 1; i >= 0; i--) {
      var element = elements.get(i);
      int priority = !element.hidden() ? 1 : element.id().equals(selected) ? 0 : 2;
      if (pass != priority) continue;
      double padding = element.hidden() ? 3 / scale() : 0;
      if (virtualX(x) < element.x() - padding || virtualX(x) >= element.x() + element.width() + padding
          || virtualY(y) < element.y() - padding || virtualY(y) >= element.y() + element.height() + padding) continue;
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
    if (divider.dragging()) moveDivider(x, y);
    if (dragging != null) move(x, y);
    stopGesture();
  }
  protected void stopGesture() { dragging = null; divider.stop(); }
  protected void mouseClicked(int x, int y, int button) {
    if (selecting && !modal()) {
      int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = selectorPopupWidth() / columns;
      int top = 51;
      if (button == 0 && inside(x, y, left(), top, selectorPopupWidth(), rows * 20)) {
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
    int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = selectorPopupWidth() / columns;
    int top = 51; List<String> ids = selectionIds();
    fill(left() - 2, top - 2, left() + selectorPopupWidth() + 2, top + rows * 20 + 2, 0xff777777);
    fill(left() - 1, top - 1, left() + selectorPopupWidth() + 1, top + rows * 20 + 1, 0xff181818);
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
  private void ghostOutline(HudPreview.Element element) {
    int l = (int)Math.floor(canvasX() + element.x() * scale()) - 1;
    int t = (int)Math.floor(canvasY() + element.y() * scale()) - 1;
    int r = (int)Math.ceil(canvasX() + (element.x() + element.width()) * scale()) + 1;
    int b = (int)Math.ceil(canvasY() + (element.y() + element.height()) * scale()) + 1;
    int colour = element.id().equals(selected) ? 0xffffff66 : 0xffcccccc;
    for (int x = l; x < r; x += 6) {
      fill(x + 1, t + 1, Math.min(r + 1, x + 4), t + 2, 0xdd000000);
      fill(x + 1, b, Math.min(r + 1, x + 4), b + 1, 0xdd000000);
      fill(x, t, Math.min(r, x + 3), t + 1, colour);
      fill(x, b - 1, Math.min(r, x + 3), b, colour);
    }
    for (int y = t; y < b; y += 6) {
      fill(l + 1, y + 1, l + 2, Math.min(b + 1, y + 4), 0xdd000000);
      fill(r, y + 1, r + 1, Math.min(b + 1, y + 4), 0xdd000000);
      fill(l, y, l + 1, Math.min(b, y + 3), colour);
      fill(r - 1, y, r, Math.min(b, y + 3), colour);
    }
  }
  protected void renderPreview(int x, int y, float delta) {
    int colour = divider.dragging() || overDivider(x, y) ? 0xffaaaaaa : 0xff686868;
    if (geometry().horizontal()) {
      int line = controlsRight() + 5, middle = (controlsTop() + controlsBottom()) / 2;
      fill(line, controlsTop(), line + 1, controlsBottom(), colour);
      fill(line - 2, middle - 12, line + 3, middle + 12, 0xff343434);
      fill(line - 1, middle - 8, line, middle + 8, colour);
      fill(line + 1, middle - 8, line + 2, middle + 8, colour);
    } else {
      int line = controlsBottom() + 5;
      fill(left(), line, right(), line + 1, colour);
      fill(width / 2 - 12, line - 2, width / 2 + 12, line + 3, 0xff343434);
      fill(width / 2 - 8, line - 1, width / 2 + 8, line, colour);
      fill(width / 2 - 8, line + 1, width / 2 + 8, line + 2, colour);
    }
    if (dragging != null) { if (!modal() && Mouse.isButtonDown(0)) move(x, y); else stopGesture(); }
    fill(previewLeft(), previewTop(), previewLeft() + previewSpan(), previewBottom(), 0xb010161d);
    clip(previewLeft(), previewTop(), previewSpan(), Math.max(0, previewBottom() - previewTop()));
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glTranslated(canvasX(), canvasY(), 0); GL11.glScaled(scale(), scale(), 1);
      fill(0, 0, width, height, 0xff26323c);
      if (background) HudPreviewBackground.draw(minecraft, width, height);
      if (dragging != null && snapping) {
        fill(width / 2, 0, width / 2 + 1, height, 0x5566bbff);
        fill(0, height / 2, width, height / 2 + 1, 0x5566bbff);
      }
      preview.draw(this, minecraft, width, height, scenario, selected);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); unclip(); }
    clip(previewLeft(), previewTop(), previewSpan(), Math.max(0, previewBottom() - previewTop()));
    try { for (var element : preview.elements()) if (element.hidden()) ghostOutline(element); }
    finally { unclip(); }
  }
}
