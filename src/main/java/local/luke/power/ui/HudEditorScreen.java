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
  private final DebugReorder reorder;
  private final DraftExitPrompt reorderExit = new DraftExitPrompt();
  private int scenario;
  private int lastNativeIndex;
  private String selected = "layout";
  private boolean snapping = true, selecting, background = true;
  private final EditorSplit divider = new EditorSplit();
  private int popupIndex;
  private HudPreview.Element dragging;
  private HudEditorToolbar toolbarCache;
  private int toolbarWidth = -1;
  private int toolbarSelectorWidth = -1;
  private boolean toolbarCompact, settingsHidden, settingsHiddenBeforeReorder, previewHidden;
  private String highlightedElement = "", highlightedSetting = "";
  private double pressX, pressY;
  private int startX, startY;

  public HudEditorScreen(PowerOptionsScreen parent, String settingId) {
    super(parent, new SettingsDraft(parent.session().settings(), HudEditorSettings::contains));
    preview = new HudPreview(draft);
    reorder = new DebugReorder(draft, id -> {
      highlightedSetting = "";
      var line = DebugLines.find(id);
      if (line != null) {
        Setting setting = editingDraft().get(line.setting());
        highlightedSetting = setting.id; revealControl(setting);
      }
    });
    collapsed.addAll(HudEditorMemory.collapsed);
    select(HudEditorSettings.tab(draft.get(settingId), preview.mode()));
    if (settingId.equals(HudEditorSettings.ENTRY) && selectionIds().contains(HudEditorMemory.selected)) {
      select(HudEditorMemory.selected); scroll = HudEditorMemory.scrolls.getOrDefault(selected, 0d);
    }
  }
  public void removed() {
    HudEditorMemory.selected = selected; HudEditorMemory.scrolls.put(selected, scroll);
    HudEditorMemory.collapsed.clear(); HudEditorMemory.collapsed.addAll(collapsed); super.removed();
  }
  private HudEditorToolbar toolbar() {
    int selector = HudEditorLayout.horizontal(width, height)
        ? HudEditorLayout.at(width, height, divider.sideWidth()).controlsWidth() : span();
    boolean compact = compact();
    if (toolbarCache == null || toolbarWidth != width || toolbarSelectorWidth != selector || toolbarCompact != compact) {
      toolbarCompact = compact;
      toolbarWidth = width;
      toolbarSelectorWidth = selector;
      toolbarCache = HudEditorToolbar.at(width, selector, text -> textRenderer == null ? text.length() * 6 : textRenderer.getWidth(text), compact);
    }
    return toolbarCache;
  }
  protected int controlsTop() { return toolbar().controlsTop(); }
  protected String title() { return "HUD editor"; }
  protected String doneLabel() { return reorder.active() ? "Save F3" : "Done"; }
  protected String cancelLabel() { return reorder.active() ? "Back" : "Cancel"; }
  public void init() { toolbarCache = null; super.init(); HudPreviewBackground.prepare(minecraft); reorder.init(minecraft, width, height); }
  public void beginReorder() {
    select("debug");
    try {
      reorder.begin(); preview.suppressDebug = true; error = "";
      previewHidden = false;
      settingsHiddenBeforeReorder = settingsHidden;
      if (compact()) { settingsHidden = true; cancelControlGesture(); }
    }
    catch (IllegalArgumentException failure) { error = failure.getMessage(); }
  }
  private void finishReorder(boolean save) { reorder.finish(save); settingsHidden = settingsHiddenBeforeReorder && !previewHidden; preview.suppressDebug = false; clearHighlight(); controlsChanged(); }
  protected SettingsDraft editingDraft() { return reorder == null ? draft : reorder.draft(); }
  protected boolean modal() { return super.modal() || reorderExit.active() || reorder != null && reorder.prompt(); }
  protected void save() { if (reorder.active()) finishReorder(true); else super.save(); }
  protected void leave() { if (reorder.active()) leaveReorder(); else super.leave(); }
  private void leaveReorder() { stopGesture(); inline.cancel(); reorderExit.leave(reorder.changed(), () -> finishReorder(false)); }
  protected String display(Setting s) { return s.id.equals("interface.debugOrder") ? reorder.active() ? "Save" : "Reorder" : super.display(s); }
  protected void edit(Setting s, int direction, int y) {
    if (s.id.equals("interface.debugOrder")) { click(); if (reorder.active()) finishReorder(true); else beginReorder(); }
    else super.edit(s, direction, y);
  }
  public void onMouseEvent() {
    int wheel = Mouse.getEventDWheel();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    int y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if (!modal() && previewVisible() && reorder.active() && wheel != 0) reorder.wheel(wheel, x, y);
    super.onMouseEvent();
  }
  private HudEditorLayout geometry() {
    return HudEditorLayout.at(width, height, HudEditorLayout.horizontal(width, height)
        ? divider.sideWidth() : divider.bottom(controlsTop(), previewBottom()), controlsTop(), !controlsVisible(), !previewVisible(), previewBottom());
  }
  private boolean compact() { return HudEditorLayout.compact(width, height); }
  protected boolean controlsVisible() { return !compact() || !settingsHidden; }
  private boolean previewVisible() { return !compact() || !previewHidden; }
  protected int previewBottom() { return height - (error.isEmpty() ? 35 : 53); }
  protected int span() { return width - 20; }
  protected int controlsLeft() { return geometry().controlsLeft(); }
  protected int controlsSpan() { return geometry().controlsWidth(); }
  protected int controlsBottom() { return geometry().controlsBottom(); }
  protected int previewLeft() { return geometry().previewLeft(); }
  protected int previewSpan() { return geometry().previewWidth(); }
  protected int previewTop() { return geometry().previewTop(); }
  protected void resetValue(Setting s) {
    s.reset(); if (s.id.equals("interface.debugOrder")) editingDraft().get("interface.debugRightOrder").reset();
  }
  protected boolean atDefault(Setting s) {
    return super.atDefault(s) && (!s.id.equals("interface.debugOrder") || super.atDefault(editingDraft().get("interface.debugRightOrder")));
  }
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
    if (reorder != null && reorder.active()) return HudEditorSections.order(editingDraft().settings().stream()
        .filter(s -> s.group.equals("Debug (F3) info") && !HudEditorSettings.internal(s)).toList());
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
    return HudEditorSections.order(draft.settings().stream().filter(s -> !HudEditorSettings.internal(s) && s.group.equals(selected.equals("version") ? "Version label" : "Debug (F3) info")).toList());
  }
  protected boolean offersPositionReset() { return !reorder.active(); }
  protected void reset() { if (reorder.active()) { editingDraft().get("interface.debugOrder").reset(); editingDraft().get("interface.debugRightOrder").reset(); } else super.reset(); }
  protected List<Setting> positionSettings() { return (selected.equals("debug") ? draft.settings().stream().filter(s -> s.id.equals("interface.debugOrder") || s.id.equals("interface.debugRightOrder")) : controls().stream()).filter(HudEditorSections::position).toList(); }
  protected String resetSubject() { return selectedLabel(); }
  protected List<Setting> allResetSettings() { return draft.settings().stream().filter(s -> !s.id.equals("infoHud.selectedCluster") && !s.id.equals("interface.debugPresets") && !s.id.equals(HudEditorSettings.COLOURS)).toList(); }
  private int nativeIndex() { return selected.startsWith("native.") ? Integer.parseInt(selected.substring(7)) : 0; }
  private List<String> selectionIds() {
    List<String> ids = new ArrayList<>(); ids.add("layout");
    if (preview.mode() == 1) ids.add("combined"); else for (int i = 0; i < 5; i++) ids.add("native." + i);
    for (int i = 0; i < 4; i++) ids.add("cluster." + i);
    ids.addAll(List.of("chat", "tweaks", "version", "debug")); return ids;
  }
  private String selectedLabel() { return selectionLabel(selected); }
  private String selectionLabel(String id) {
    return HudEditorSettings.tabLabel(id);
  }
  private void select(String id) {
    boolean changed = !selected.equals(id);
    clearHighlight();
    if (changed) HudEditorMemory.scrolls.put(selected, scroll);
    selected = id; selecting = false; stopGesture();
    if (id.startsWith("native.")) lastNativeIndex = nativeIndex();
    if (changed) { controlsChanged(); scroll = HudEditorMemory.scrolls.getOrDefault(id, 0d); }
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
  private int selectorWidth() {
    return toolbar().selectorWidth();
  }
  private int panelToggleX() { return right() - 40; }
  private boolean panelToggleAt(int x, int y) { return compact() && inside(x, y, panelToggleX(), 30, 18, 18); }
  private boolean previewToggleAt(int x, int y) { return compact() && inside(x, y, panelToggleX() + 22, 30, 18, 18); }
  private void toggleSettings() {
    click(); stopGesture(); cancelControlGesture(); reorder.closePicker();
    settingsHidden = !settingsHidden;
    if (settingsHidden) previewHidden = false;
    if (controlsVisible() && !highlightedSetting.isEmpty()) revealControl(editingDraft().get(highlightedSetting));
  }
  private void togglePreview() {
    click(); stopGesture(); cancelControlGesture(); reorder.closePicker();
    previewHidden = !previewHidden;
    if (previewHidden) {
      settingsHidden = false;
      if (!highlightedSetting.isEmpty()) revealControl(editingDraft().get(highlightedSetting));
    }
  }
  private int selectorPopupWidth() { return geometry().horizontal() ? controlsSpan() : span(); }
  protected String hoverHelp(int x, int y) {
    if (panelToggleAt(x, y)) return controlsVisible() ? "Hide settings" : "Show settings";
    if (previewToggleAt(x, y)) return previewVisible() ? "Hide preview" : "Show preview";
    if (previewVisible() && reorder.active() && !reorder.help(x, y).isEmpty()) return reorder.help(x, y);
    if (inside(x, y, left(), 30, selectorWidth(), 18)) return selectedLabel();
    for (var toggle : toolbar().toggles()) if (inside(x, y, toggle.x(), toggle.y(), toggle.width(), 18)) return toggleLabel(toggle.id()) + "\n" + switch (toggle.id()) {
      case "snap" -> "Snap dragged elements to the screen edges and centre.";
      case "f3" -> "Show F3 information in the preview.";
      case "background" -> "Show the forest scene in the preview.";
      default -> "Show or hide this preview element without changing its settings.";
    };
    return "";
  }
  private String toggleName(String id) {
    return switch (id) { case "f3" -> "F3"; case "background" -> "Bg"; case "toast" -> "Toast";
      case "chat" -> "Chat"; case "tweaks" -> toolbar().compact() ? "Tweaks" : "Active tweaks"; default -> "Snap"; };
  }
  private boolean toggleEnabled(String id) {
    return switch (id) { case "f3" -> scenario == 2; case "background" -> background; case "toast" -> preview.showToast;
      case "chat" -> preview.showChat; case "tweaks" -> preview.showTweaks; default -> snapping; };
  }
  private String toggleLabel(String id) {
    return toggleName(id) + (toggleEnabled(id) ? ": On" : ": Off");
  }
  protected void renderToolbar(int x, int y) {
    int sw = selectorWidth(); button("", left(), 30, sw, 18, x, y, true);
    text(fit(selectedLabel(), sw - 24), left() + 5, 35, 0xe0e0e0); chevron(left() + sw - 13, 37, selecting, 0xffdddddd);
    if (compact()) {
      iconButton(controlsVisible() ? "collapse" : "expand", panelToggleX(), 30, 18, x, y, true);
      iconButton(previewVisible() ? "eye" : "eyeClosed", panelToggleX() + 22, 30, 18, x, y, true);
    }
    if (!toolbar().compact()) text(HudEditorToolbar.LABEL, toolbar().labelX(), toolbar().labelY(), 0xb0b0b0);
    for (var toggle : toolbar().toggles()) {
      if (toolbar().compact()) tabButton(toggleName(toggle.id()), toggle.x(), toggle.y(), toggle.width(), 18, x, y, true, toggleEnabled(toggle.id()));
      else button(toggleLabel(toggle.id()), toggle.x(), toggle.y(), toggle.width(), 18, x, y, true);
    }
  }
  private boolean overDivider(int x, int y) {
    if (!controlsVisible() || !previewVisible()) return false;
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
    if (inside(x, y, left(), 30, selectorWidth(), 18)) {
      click(); selecting = !selecting; popupIndex = Math.max(0, selectionIds().indexOf(selected)); stopGesture(); return true;
    }
    for (var toggle : toolbar().toggles()) if (inside(x, y, toggle.x(), toggle.y(), toggle.width(), 18)) {
      click();
      switch (toggle.id()) {
        case "f3" -> scenario = scenario == 2 ? 0 : 2;
        case "background" -> background = !background;
        case "toast" -> preview.showToast = !preview.showToast;
        case "chat" -> preview.showChat = !preview.showChat;
        case "tweaks" -> preview.showTweaks = !preview.showTweaks;
        default -> snapping = !snapping;
      }
      return true;
    }
    return false;
  }
  private double scale() { return geometry().scale(width, height); }
  private double canvasX() { return geometry().canvasX(width, height); }
  private double canvasY() { return geometry().canvasY(width, height); }
  private double virtualX(int x) { return (x - canvasX()) / scale(); }
  private double virtualY(int y) { return (y - canvasY()) / scale(); }
  private static String generalSetting(String elementId) {
    return switch (elementId) {
      case "crosshair" -> HudEditorSettings.HUD + "disableCrosshair";
      case "controller" -> HudEditorSettings.HUD + "drawXboxXAndYButtons";
      case "toast" -> HudEditorSettings.TOAST;
      default -> "";
    };
  }
  protected void previewClick(int x, int y, int button) {
    if (!previewVisible() || button != 0 || !inside(x, y, previewLeft(), previewTop(), previewSpan(), previewBottom() - previewTop())) return;
    List<HudPreview.Element> elements = preview.elements();
    for (int pass = 0; pass < 3; pass++) for (int i = elements.size() - 1; i >= 0; i--) {
      var element = elements.get(i);
      if (element.hidden() && !element.id().equals(selected)) continue;
      int priority = !element.hidden() ? generalSetting(element.id()).isEmpty() ? 2 : 0 : 1;
      if (pass != priority) continue;
      double padding = element.hidden() ? 3 / scale() : 0;
      if (virtualX(x) < element.x() - padding || virtualX(x) >= element.x() + element.width() + padding
          || virtualY(y) < element.y() - padding || virtualY(y) >= element.y() + element.height() + padding) continue;
      String settingId = generalSetting(element.id());
      if (!settingId.isEmpty()) {
        select("layout"); revealControl(draft.get(settingId));
        highlightedElement = element.id(); highlightedSetting = settingId;
        return;
      }
      if (!element.id().equals(selected)) { select(element.id()); return; }
      if (element.offsetX() != null || element.offsetY() != null) {
        draft.checkpoint(); dragging = element; pressX = virtualX(x); pressY = virtualY(y);
        startX = element.offsetX() == null ? 0 : preview.integer(element.offsetX());
        startY = element.offsetY() == null ? 0 : preview.integer(element.offsetY());
      }
      return;
    }
    select("layout");
  }
  protected void finishGesture(int x, int y, int button) {
    reorder.release(x, y, button);
    if (button != 0) return;
    if (divider.dragging()) moveDivider(x, y);
    if (dragging != null) move(x, y);
    stopGesture();
  }
  protected void stopGesture() { dragging = null; divider.stop(); if (reorder != null) reorder.cancelDrag(); }
  protected void mouseClicked(int x, int y, int button) {
    if (reorderExit.click(this, x, y, button, () -> finishReorder(true), () -> finishReorder(false))) return;
    if (!modal() && !selecting && button == 0 && panelToggleAt(x, y)) { toggleSettings(); return; }
    if (!modal() && !selecting && button == 0 && previewToggleAt(x, y)) { togglePreview(); return; }
    if (reorder.active() && !modal() && !selecting
        && toolbar().toggles().stream().anyMatch(t -> inside(x, y, t.x(), t.y(), t.width(), 18))) {
      toolbarClick(x, y, button); return;
    }
    if (reorder.active() && !super.modal()) {
      if (previewVisible() && reorder.press(x, y, button)) return;
      Setting order = editingDraft().get("interface.debugOrder");
      if (controlsVisible() && inside(x, y, controlX(), controlY(order), controlWidth(order), 18)) { click(); finishReorder(true); return; }
      boolean controls = inside(x, y, controlsLeft(), controlsTop(), controlsSpan(), controlsBottom() - controlsTop());
      boolean footer = y >= height - 31;
      if (!controls && !footer) { leaveReorder(); return; }
    }
    if (selecting && !modal()) {
      int rows = selectorRows(), columns = (selectionIds().size() + rows - 1) / rows, cellWidth = selectorPopupWidth() / columns;
      int top = 51;
      if (button == 0 && inside(x, y, left(), top, selectorPopupWidth(), rows * 20)) {
        int index = (x - left()) / cellWidth * rows + (y - top) / 20;
        if (index < selectionIds().size()) { click(); select(selectionIds().get(index)); }
      }
      selecting = false; return;
    }
    super.mouseClicked(x, y, button);
  }
  protected void keyPressed(char c, int key) {
    if (reorderExit.key(key)) return;
    if (reorder.active() && reorder.key(c, key)) return;
    if (key == Keyboard.KEY_ESCAPE && reorder.active() && !super.modal() && !inline.active()) { leaveReorder(); return; }
    if (selecting && !modal()) {
      if (key == Keyboard.KEY_ESCAPE) selecting = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        popupIndex = Math.floorMod(popupIndex + (key == Keyboard.KEY_UP ? -1 : 1), selectionIds().size());
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); select(selectionIds().get(popupIndex)); }
      else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT)
        popupIndex = Math.floorMod(popupIndex + (key == Keyboard.KEY_LEFT ? -selectorRows() : selectorRows()), selectionIds().size());
      return;
    }
    super.keyPressed(c, key);
  }
  private int selectorRows() { return Math.max(1, Math.min(selectionIds().size(), (height - 96) / 20)); }
  public void render(int x, int y, float delta) {
    super.render(selecting ? -1000 : x, selecting ? -1000 : y, delta);
    if (reorder.active()) {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
      try {
        GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
        reorder.drawPrompt(x, y);
        reorderExit.render(this, "Save F3 changes?", x, y);
      } finally { GL11.glPopAttrib(); }
    }
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
      if (id.equals(selected)) fill(px + 2, py + 3, px + 4, py + 16, 0xff268bff);
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
    if (!previewVisible()) return false;
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
  private void clearHighlight() { highlightedElement = ""; highlightedSetting = ""; }
  private boolean highlighting() { return selected.equals("layout") && !highlightedElement.isEmpty(); }
  protected void settingClicked(Setting setting) { clearHighlight(); if (reorder.active()) reorder.clearSelection(); }
  protected void renderControl(Setting s, int y, int mx, int my) {
    super.renderControl(s, y, mx, my);
    if (s.id.equals(highlightedSetting)) {
      int l = controlsLeft() + 2, r = controlsRight() - 9;
      fill(l, y - 1, r, y, 0xffffff66); fill(l, y + 18, r, y + 19, 0xffffff66);
      fill(l, y - 1, l + 1, y + 19, 0xffffff66); fill(r - 1, y - 1, r, y + 19, 0xffffff66);
    }
  }
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
    if (!previewVisible()) return;
    int colour = divider.dragging() || overDivider(x, y) ? 0xffaaaaaa : 0xff686868;
    if (controlsVisible() && geometry().horizontal()) {
      int line = controlsRight() + 5, middle = (controlsTop() + controlsBottom()) / 2;
      fill(line, controlsTop(), line + 1, controlsBottom(), colour);
      fill(line - 2, middle - 12, line + 3, middle + 12, 0xff343434);
      fill(line - 1, middle - 8, line, middle + 8, colour);
      fill(line + 1, middle - 8, line + 2, middle + 8, colour);
    } else if (controlsVisible()) {
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
      preview.draw(this, minecraft, width, height, scenario, highlighting() ? highlightedElement : selected, background);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); unclip(); }
    clip(previewLeft(), previewTop(), previewSpan(), Math.max(0, previewBottom() - previewTop()));
    try { for (var element : preview.elements()) if (element.hidden() && element.id().equals(selected)) ghostOutline(element); }
    finally { unclip(); }
    reorder.draw(previewLeft(), previewTop(), previewSpan(), previewBottom() - previewTop(), x, y);
  }
}
