package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.Setting;
import local.luke.power.creative.CreativeTabsData;
import local.luke.power.creativeui.CreativeTabsBackend;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** An isolated catalog layout draft. No native inventory or cursor stack is accessed. */
public final class CreativeTabsScreen extends UiScreen {
  private final Screen parent;
  private final Setting setting;
  private final Runnable accepted;
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final ItemIcons icons = new ItemIcons();
  private final ScrollBar listBar = new ScrollBar(), gridBar = new ScrollBar();
  private final SlotDoubleClick clicks = new SlotDoubleClick();
  private final CreativeTabsHistory history = new CreativeTabsHistory();
  private final Map<String, ItemCatalogue.Entry> entries = new HashMap<>();
  private final TextInput title = new TextInput("", 64);
  private CreativeTabsData data;
  private JsonObject original;
  private String error = "", problem = "";
  private int selected, listScroll, gridScroll, anchor = -1;
  private boolean rename, nativeGroups;
  private CreativeTabsData nativeGroupData;
  private int nativeFocus;
  private final SortedSet<Integer> cells = new TreeSet<>();
  private int dragCell = -1, dragTab = -1, pressX, pressY;
  private boolean dragging, selectionPress, selectingCells, deselectingCells, clearingCells, movingCells, doubleClick;
  private int dragButton = -1;
  private final SortedSet<Integer> clearCells = new TreeSet<>();
  private Set<Integer> selectionBeforePress = Set.of();
  private List<String> dragSource;
  private Set<Integer> dragSelection;

  public CreativeTabsScreen(Screen parent, Setting setting, Runnable accepted) {
    this.parent = parent; this.setting = setting; this.accepted = accepted;
    try {
      data = CreativeTabsData.read(setting.value.getAsJsonObject());
      if (!data.customized()) data = defaultDraft();
      original = data.json(); select(0);
    } catch (RuntimeException e) { problem = Objects.toString(e.getMessage(), "Creative tabs are unavailable"); }
  }
  private CreativeTabsData defaultDraft() {
    CreativeTabsData defaults = CreativeTabsData.read(CreativeTabsBackend.defaultLayout());
    for (int i = 0; i < defaults.size(); i++) {
      List<String> slots = CreativeTabsData.slots(defaults.tab(i));
      while (slots.size() < 64 || slots.size() % 8 != 0) slots.add("");
      CreativeTabsData.slots(defaults.tab(i), slots);
    }
    return defaults;
  }
  private int span() { return Math.min(470, width - 20); }
  private int left() { return (width - span()) / 2; }
  private int listWidth() { return Math.max(92, Math.min(160, span() - 180)); }
  private int tabButtonWidth() { return Math.max(68, (listWidth() - 8) / 2); }
  private int tabButtonX(int index) { return left() + index * (tabButtonWidth() + 4); }
  private int fillGroupX() { return Math.max(left() + listWidth(), tabButtonX(1) + tabButtonWidth() + 4); }
  private int fillGroupWidth(int limit) { return Math.min(limit, left() + span() - fillGroupX()); }
  private int gridLeft() { return left() + listWidth() + (span() - listWidth() - 144) / 2; }
  private int gridTop() { return 85; }
  private int gridBottom() { return height - 60; }
  private int rowControlsY() { return height - 56; }
  private int footerWidth() { return Math.min(76, (width - 28) / 4); }
  private int footerX(int index) { return (width - footerWidth() * 4 - 12) / 2 + index * (footerWidth() + 4); }
  private ScrollBar.Track listTrack() { return new ScrollBar.Track(left() + listWidth() - 7, 59, Math.max(0, visibleListRows() * 23), data == null ? 0 : data.size() * 23, listScroll * 23); }
  private ScrollBar.Track gridTrack() { return new ScrollBar.Track(gridLeft() + 148, gridTop(), Math.max(0, gridBottom() - gridTop()), hasTab() ? paddedSlots().size() / 8 * 18 : 0, gridScroll); }
  private void clampScroll() {
    listScroll = Math.max(0, Math.min(listScroll, Math.max(0, data.size() - visibleListRows())));
    gridScroll = (int)Math.max(0, Math.min(gridScroll, gridTrack().maximum()));
  }
  private int listBottom() { return height - 72; }
  private int visibleListRows() { return Math.max(1, (listBottom() - 59) / 23); }
  private boolean editable() { return data != null && problem.isEmpty(); }
  private JsonObject tab() { return data.tab(selected); }
  private boolean hasTab() { return editable() && data.size() > 0; }
  private List<String> paddedSlots() {
    List<String> values = CreativeTabsData.slots(tab());
    int length = Math.max(8, ((values.size() + 7) / 8) * 8);
    while (values.size() < length) values.add("");
    return values;
  }
  private CreativeTabsHistory.State snapshot() { return new CreativeTabsHistory.State(data.json(), selected, listScroll, gridScroll, cells, anchor); }
  private boolean edit(Runnable change) {
    CreativeTabsHistory.State before = snapshot();
    try {
      change.run(); CreativeTabsData.read(data.json()); history.record(before, data.json()); entries.clear(); error = ""; return true;
    } catch (RuntimeException invalid) {
      data = CreativeTabsData.read(before.document()); error = Objects.toString(invalid.getMessage(), "Could not edit Creative tabs"); return false;
    }
  }
  private void storeSlots(List<String> values) { edit(() -> CreativeTabsData.slots(tab(), values)); }
  private boolean pendingTitle() { return hasTab() && rename && !title.text().strip().equals(tab().get("title").getAsString()); }
  private void undo() {
    if (pendingTitle()) { cancelTitle(); error = ""; return; }
    cancelTitle(); cancelDrag();
    CreativeTabsHistory.State state = history.undo();
    if (state == null) return;
    data = CreativeTabsData.read(state.document()); select(state.selected()); listScroll = state.listScroll(); gridScroll = state.gridScroll();
    cells.addAll(state.cells()); anchor = state.anchor(); clampScroll(); error = ""; entries.clear();
  }
  private void removeRow() {
    if (!hasTab()) return;
    List<String> values = paddedSlots();
    if (values.size() <= 8) return;
    int row = anchor >= 0 ? anchor / 8 : values.size() / 8 - 1;
    row = Math.max(0, Math.min(row, values.size() / 8 - 1));
    values.subList(row * 8, row * 8 + 8).clear(); storeSlots(values); cells.clear(); anchor = -1; clampScroll();
  }
  private void select(int index) {
    selected = Math.max(0, Math.min(index, data.size() - 1));
    cells.clear(); anchor = -1; gridScroll = 0; rename = false; title.focused = false; cancelDrag(); clicks.cancel();
    if (data.size() > 0) title.setText(tab().get("title").getAsString());
  }
  private void cancelDrag() { dragCell = dragTab = dragButton = -1; dragging = selectingCells = deselectingCells = clearingCells = movingCells = doubleClick = false; dragSource = null; dragSelection = null; clearCells.clear(); }
  @Override public void init() { Keyboard.enableRepeatEvents(true); }
  @Override public void removed() { Keyboard.enableRepeatEvents(false); TextDrag.cancel(); cancelDrag(); clicks.cancel(); listBar.release(); gridBar.release(); }
  private boolean changed() {
    return editable() && (!data.json().equals(original)
        || rename && !title.text().strip().equals(tab().get("title").getAsString()));
  }
  private void leave() { cancelDrag(); exit.leave(changed(), () -> showScreen(parent)); }
  private void cancelTitle() {
    rename = false; title.focused = false;
    if (hasTab()) title.setText(tab().get("title").getAsString());
    TextDrag.cancel();
  }
  private boolean commitTitle() {
    if (!rename) return true;
    String value = title.text().strip();
    if (value.isEmpty()) { error = "Enter a tab name."; return false; }
    if (!edit(() -> tab().addProperty("title", value))) return false;
    rename = false; title.focused = false; error = ""; return true;
  }
  private void save() {
    if (!editable() || !commitTitle()) return;
    try {
      if (changed()) {
        data.customized(true);
        JsonObject value = CreativeTabsData.read(data.json()).json();
        setting.value = value; accepted.run();
      }
      showScreen(parent);
    } catch (RuntimeException e) { error = Objects.toString(e.getMessage(), "Could not save Creative tabs"); }
  }
  private void picker(String current, java.util.function.Consumer<String> chosen) {
    cancelDrag();
    showScreen(new ItemPickerScreen(this, "Choose item", ItemCatalogue.Domain.FILTER,
        current.isEmpty() ? List.of() : List.of(current), false,
        new ItemPickerScreen.Options(true, entry -> entry.icon() != null), values -> {
          ItemCatalogue.Entry value = ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, values.get(0));
          chosen.accept(value.id() + ":" + Math.max(0, value.damage())); entries.clear();
        }));
  }
  private void addTab() {
    if (!commitTitle()) return;
    try {
      edit(() -> data.append(CreativeTabsData.tab(UUID.randomUUID().toString(), "New tab", "1", Collections.nCopies(64, ""))));
      select(data.size() - 1); listScroll = Math.max(0, data.size() - visibleListRows());
    } catch (IllegalArgumentException e) { error = e.getMessage(); }
  }
  private CreativeTabsData nativeGroupData() {
    if (nativeGroupData == null) nativeGroupData = CreativeTabsData.read(CreativeTabsBackend.nativeLayout());
    return nativeGroupData;
  }
  private List<String> nativeLabels() {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < Math.min(7, nativeGroupData().size()); i++) labels.add(nativeGroupData().tab(i).get("title").getAsString());
    return labels;
  }
  private String identity(String value) {
    try {
      ItemCatalogue.Entry entry = ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, value);
      return entry.id() + ":" + Math.max(0, entry.damage());
    } catch (IllegalArgumentException unavailable) { return value; }
  }
  private void fillNative(int index) {
    if (!hasTab()) return;
    try {
      CreativeTabsData groups = nativeGroupData();
      List<String> values = paddedSlots(), additions = new ArrayList<>();
      Set<String> included = new HashSet<>();
      for (String value : values) if (!value.isEmpty()) included.add(identity(value));
      for (String value : CreativeTabsData.slots(groups.tab(index)))
        if (!value.isEmpty() && included.add(identity(value))) additions.add(value);
      int empty = (int)values.stream().filter(String::isEmpty).count();
      int addedRows = (Math.max(0, additions.size() - empty) + 7) / 8;
      if (values.size() + addedRows * 8 > CreativeTabsData.MAX_ROWS * 8)
        throw new IllegalArgumentException("This group needs more space. Clear some cells before filling it.");
      values.addAll(Collections.nCopies(addedRows * 8, ""));
      int next = 0;
      for (int cell = 0; cell < values.size() && next < additions.size(); cell++)
        if (values.get(cell).isEmpty()) values.set(cell, additions.get(next++));
      // Validate the complete result before changing any title, icon, slot or other tab.
      CreativeTabsData candidate = CreativeTabsData.read(data.json());
      CreativeTabsData.slots(candidate.tab(selected), values);
      CreativeTabsData.read(candidate.json());
      storeSlots(values); nativeGroups = false; error = "";
    } catch (RuntimeException e) { nativeGroups = false; error = e.getMessage(); }
  }

  private int gridCell(int x, int y) {
    if (!hasTab() || !inside(x, y, gridLeft(), gridTop(), 144, Math.max(0, gridBottom() - gridTop()))) return -1;
    int row = (y - gridTop() + gridScroll) / 18;
    int index = row * 8 + (x - gridLeft()) / 18;
    return index < paddedSlots().size() ? index : -1;
  }
  private int tabRow(int x, int y) {
    if (!editable() || !inside(x, y, left(), 59, listWidth() - 14, Math.max(0, visibleListRows() * 23))) return -1;
    int row = listScroll + (y - 59) / 23;
    return row < data.size() ? row : -1;
  }
  private boolean ctrl() { return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL); }
  private boolean shift() { return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT); }
  @Override protected void keyPressed(char character, int key) {
    // Native mouse batches precede queued modifier presses in the same tick.
    if (local.luke.power.input.Chord.modifier(key) != 0) return;
    cancelDrag(); clicks.cancel();
    if (exit.key(key)) return;
    if (nativeGroups) {
      if (key == Keyboard.KEY_ESCAPE) nativeGroups = false;
      else if (key == Keyboard.KEY_UP) nativeFocus = Math.max(0, nativeFocus - 1);
      else if (key == Keyboard.KEY_DOWN) nativeFocus = Math.min(nativeLabels().size() - 1, nativeFocus + 1);
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); fillNative(nativeFocus); }
      return;
    }
    if (ctrl() && key == Keyboard.KEY_Z) { undo(); return; }
    if (rename) {
      if (key == Keyboard.KEY_ESCAPE) { cancelTitle(); return; }
      if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); commitTitle(); return; }
      title.key(character, key); return;
    }
    if (key == Keyboard.KEY_ESCAPE) { leave(); return; }
    if (!hasTab()) return;
    if ((key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) && !cells.isEmpty()) {
      List<String> values = paddedSlots(); for (int index : cells) values.set(index, ""); storeSlots(values); return;
    }
    if (ctrl() && key == Keyboard.KEY_A) {
      cells.clear(); for (int i = 0; i < paddedSlots().size(); i++) cells.add(i);
    }
  }
  @Override public void onMouseEvent() {
    super.onMouseEvent();
    layerWheel(Mouse.getEventDWheel());
  }
  @Override public void layerWheel(int wheel) {
    if (wheel == 0 || exit.active() || nativeGroups || !editable()) return;
    cancelDrag(); clicks.cancel();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (x < left() + listWidth()) listScroll = Math.max(0, Math.min(Math.max(0, data.size() - visibleListRows()), listScroll - Integer.signum(wheel)));
    else if (hasTab()) gridScroll = Math.max(0, Math.min(Math.max(0, paddedSlots().size() / 8 * 18 - Math.max(0, gridBottom() - gridTop())), gridScroll - Integer.signum(wheel) * 18));
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    cancelDrag();
    if (gridCell(x, y) < 0) clicks.cancel();
    if (exit.click(this, x, y, button, this::save, () -> showScreen(parent))) return;
    if (nativeGroups) {
      if (button != 0) return;
      for (int i = 0; i < nativeLabels().size(); i++)
        if (inside(x, y, fillGroupX(), 52 + i * 20, fillGroupWidth(180), 20)) { click(); fillNative(i); return; }
      nativeGroups = false; return;
    }
    if (rename) {
      int nameLeft = left() + listWidth() + 24, nameWidth = span() - listWidth() - 46;
      boolean nameHit = inside(x, y, nameLeft, 59, nameWidth, 18);
      boolean checkHit = inside(x, y, left() + span() - 18, 58, 18, 20);
      boolean footerHit = inside(x, y, footerX(0), height - 28, footerWidth() * 4 + 12, 20);
      if (!nameHit && !checkHit && !footerHit) cancelTitle();
    }
    if (button == 0 && inside(x, y, footerX(1), height - 28, footerWidth(), 20)) { click(); leave(); return; }
    if (!editable()) return;
    if (button == 0 && (history.available() || pendingTitle()) && inside(x, y, footerX(2), height - 28, footerWidth(), 20)) { click(); undo(); return; }
    if (button == 0 && listBar.press(listTrack(), x, y)) { clicks.cancel(); listScroll = (int)Math.round(listBar.drag(listTrack(), y, true) / 23); return; }
    if (button == 0 && gridBar.press(gridTrack(), x, y)) { clicks.cancel(); gridScroll = (int)gridBar.drag(gridTrack(), y, true); return; }
    if (button == 0 && inside(x, y, footerX(3), height - 28, footerWidth(), 20)) { click(); save(); return; }
    if (button == 0 && inside(x, y, tabButtonX(0), 30, tabButtonWidth(), 20)) { click(); addTab(); return; }
    if (button == 0 && hasTab() && inside(x, y, fillGroupX(), 30, fillGroupWidth(144), 20)) { click(); if (commitTitle()) { nativeGroupData(); nativeFocus = 0; nativeGroups = true; } return; }
    if (button == 0 && inside(x, y, tabButtonX(1), 30, tabButtonWidth(), 20) && hasTab()) {
      click(); edit(() -> data.remove(selected)); select(selected); listScroll = Math.min(listScroll, Math.max(0, data.size() - visibleListRows())); return;
    }
    if (button == 0 && inside(x, y, footerX(0), height - 28, footerWidth(), 20)) {
      click(); edit(() -> data = defaultDraft()); select(0); listScroll = 0; return;
    }
    int row = tabRow(x, y);
    if (row >= 0 && button == 0) {
      clicks.cancel();
      if (!commitTitle()) return;
      if (row != selected) { click(); select(row); }
      dragTab = row; pressX = x; pressY = y; return;
    }
    if (!hasTab()) return;
    int panelLeft = left() + listWidth(), nameLeft = panelLeft + 24, nameWidth = span() - listWidth() - 46;
    if (inside(x, y, nameLeft, 59, nameWidth, 18) && (button == 0 || button == 1)) {
      if (!rename) { title.setText(tab().get("title").getAsString()); rename = true; }
      title.focused = true; title.click(x, button); return;
    }
    if (button == 0 && inside(x, y, left() + span() - 18, 58, 18, 20)) {
      click();
      if (rename) commitTitle();
      else { title.setText(tab().get("title").getAsString()); rename = true; title.focused = true; }
      return;
    }
    if (!commitTitle()) return;
    if (button == 0 && inside(x, y, panelLeft, 58, 20, 20)) { click(); picker(tab().get("icon").getAsString(), value -> edit(() -> tab().addProperty("icon", value))); return; }
    if (button == 0 && inside(x, y, gridLeft(), rowControlsY(), 70, 18)) {
      List<String> values = paddedSlots();
      if (values.size() < CreativeTabsData.MAX_ROWS * 8) { click(); values.addAll(Collections.nCopies(8, "")); storeSlots(values); } return;
    }
    if (button == 0 && inside(x, y, gridLeft() + 74, rowControlsY(), 70, 18) && paddedSlots().size() > 8) {
      click(); removeRow(); return;
    }
    int cell = gridCell(x, y);
    if (cell < 0 || button != 0 && button != 1) return;
    if (button == 1) {
      clicks.cancel();
      if (ctrl()) {
        clearingCells = true; dragButton = button; dragCell = cell; pressX = x; pressY = y;
        dragSource = paddedSlots(); clearCells.add(cell);
      } else { List<String> values = paddedSlots(); values.set(cell, ""); storeSlots(values); }
      return;
    }
    selectingCells = ctrl();
    selectionPress = shift() || selectingCells;
    boolean alreadySelected = cells.contains(cell);
    deselectingCells = selectingCells && alreadySelected;
    selectionBeforePress = Set.copyOf(cells);
    doubleClick = !selectionPress && clicks.press(cell, button, Mouse.getEventNanoseconds());
    if (selectionPress) clicks.cancel();
    if (shift() && anchor >= 0) {
      if (!ctrl()) cells.clear();
      for (int i = Math.min(anchor, cell); i <= Math.max(anchor, cell); i++) cells.add(i);
    } else if (ctrl()) { if (!cells.remove(cell)) cells.add(cell); anchor = cell; }
    else { if (!alreadySelected) { cells.clear(); cells.add(cell); } anchor = cell; }
    dragCell = cell; dragButton = button; pressX = x; pressY = y; dragging = false;
    dragSource = paddedSlots(); dragSelection = Set.copyOf(cells);
    movingCells = !selectionPress && !dragSource.get(cell).isEmpty();
  }
  private void updateDrag(int x, int y) {
    if (dragCell < 0 && dragTab < 0) return;
    dragging |= Math.abs(x - pressX) + Math.abs(y - pressY) >= 4;
    if (clearingCells) {
      int destination = gridCell(x, y);
      if (destination >= 0) for (int index = Math.min(dragCell, destination); index <= Math.max(dragCell, destination); index++) clearCells.add(index);
      return;
    }
    if (!dragging) return;
    clicks.cancel();
    if (dragCell >= 0 && selectingCells) {
      int destination = gridCell(x, y);
      if (destination < 0) return;
      cells.clear(); cells.addAll(selectionBeforePress);
      for (int index = Math.min(dragCell, destination); index <= Math.max(dragCell, destination); index++) {
        if (deselectingCells) cells.remove(index); else cells.add(index);
      }
    }
  }
  @Override protected void mouseReleased(int x, int y, int button) {
    if (button == 0) {
      // Beta also dispatches motion and wheel events here with button -1.
      if (!exit.active() && Display.isActive()) updateScroll(y);
      listBar.release(); gridBar.release();
    }
    if (button != 0 && !(clearingCells && button == dragButton)) return;
    if (exit.active() || !Display.isActive()) { cancelDrag(); clicks.cancel(); return; }
    if (clearingCells && button != dragButton) return;
    updateDrag(x, y);
    if (clearingCells) {
      List<String> values = new ArrayList<>(dragSource);
      for (int index : clearCells) values.set(index, "");
      storeSlots(values); cancelDrag(); return;
    }
    if (dragTab >= 0) {
      int destination = tabRow(x, y);
      if (dragging && destination >= 0) { edit(() -> data.move(dragTab, destination)); selected = destination; }
      cancelDrag(); return;
    }
    if (dragCell >= 0) {
      int source = dragCell, destination = gridCell(x, y);
      if (dragging && movingCells && destination >= 0 && dragSelection != null && !dragSelection.isEmpty()) {
        int delta = destination - source;
        try {
          storeSlots(CreativeTabsData.moveCells(dragSource, dragSelection, delta));
          cells.clear(); for (int value : dragSelection) cells.add(value + delta); anchor = destination;
        } catch (IllegalArgumentException e) { error = e.getMessage(); }
      } else if (!dragging && doubleClick && destination == source) {
        List<String> values = paddedSlots();
        click(); picker(values.get(source), value -> { List<String> next = paddedSlots(); next.set(source, value); storeSlots(next); });
      } else if (!dragging && !selectionPress && destination == source) {
        cells.clear(); cells.add(source); anchor = source;
      }
      cancelDrag();
    }
  }
  private void drawItem(String value, int x, int y) {
    if (value.isEmpty()) return;
    try {
      ItemCatalogue.Entry entry = entries.computeIfAbsent(value, key -> ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, key));
      icons.draw(this, minecraft, entry, x, y);
    } catch (IllegalArgumentException unavailable) { text("?", x + 5, y + 4, 0xff8888); }
  }
  private void updateScroll(int y) {
    listScroll = (int)Math.round(listBar.drag(listTrack(), y, true) / 23);
    gridScroll = (int)gridBar.drag(gridTrack(), y, true);
  }
  @Override public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    if (!Display.isActive()) { cancelDrag(); clicks.cancel(); listBar.release(); gridBar.release(); }
    if (!exit.active() && !nativeGroups && Display.isActive()) {
      updateScroll(my);
      updateDrag(mx, my);
    }
    if (exit.active() || nativeGroups) { mx = -100; my = -100; }
    renderBackground(); titleBar("Creative tabs");
    button("Add tab", tabButtonX(0), 30, tabButtonWidth(), 20, mx, my, editable() && data.size() < CreativeTabsData.MAX_TABS);
    WaypointSetDropdown.button(this, "Fill group", fillGroupX(), 30, fillGroupWidth(144), mx, my, hasTab(), nativeGroups);
    button("Remove tab", tabButtonX(1), 30, tabButtonWidth(), 20, mx, my, hasTab());
    String tip = "";
    if (editable()) {
      clip(left(), 59, listWidth() - 14, Math.max(0, visibleListRows() * 23));
      for (int i = listScroll; i < Math.min(data.size(), listScroll + visibleListRows()); i++) {
        int y = 59 + (i - listScroll) * 23;
        boolean hover = tabRow(mx, my) == i;
        if (i == selected || hover) fill(left(), y, left() + listWidth() - 14, y + 21, i == selected ? 0x80445566 : 0x50333333);
        drawItem(data.tab(i).get("icon").getAsString(), left() + 2, y + 2);
        text(fit(data.tab(i).get("title").getAsString(), listWidth() - 38), left() + 22, y + 7, 0xeeeeee);
        if (dragging && dragTab >= 0 && hover) fill(left(), y, left() + listWidth() - 14, y + 1, 0xffffff88);
      }
      unclip(); listBar.render(this, listTrack());
      if (hasTab()) {
        int panelLeft = left() + listWidth(), nameWidth = span() - listWidth() - 46;
        button("", panelLeft, 58, 20, 20, mx, my, true); drawItem(tab().get("icon").getAsString(), panelLeft + 2, 60);
        if (rename) { input(title, panelLeft + 24, 59, nameWidth, mx, my, "Tab name"); iconButton("check", left() + span() - 18, 58, 18, 20, mx, my, true, false); }
        else {
          input(title, panelLeft + 24, 59, nameWidth, mx, my, "Tab name");
          iconButton("edit", left() + span() - 18, 58, 18, 20, mx, my, true, false);
        }
        List<String> values = paddedSlots();
        clip(gridLeft(), gridTop(), 144, Math.max(0, gridBottom() - gridTop()));
        int first = gridScroll / 18, last = Math.min(values.size() / 8, (gridScroll + Math.max(0, gridBottom() - gridTop()) + 17) / 18);
        int hover = gridCell(mx, my);
        for (int row = first; row < last; row++) for (int col = 0; col < 8; col++) {
          int index = row * 8 + col, x = gridLeft() + col * 18, y = gridTop() + row * 18 - gridScroll;
          fill(x, y, x + 18, y + 18, 0xff666666); fill(x + 1, y + 1, x + 17, y + 17, 0xff252525);
          if (!clearingCells || !clearCells.contains(index)) drawItem(values.get(index), x + 1, y + 1);
          if (clearingCells && clearCells.contains(index)) fill(x + 1, y + 1, x + 17, y + 17, 0x80bb5555);
          if (cells.contains(index) || index == hover) fill(x + 1, y + 1, x + 17, y + 17, cells.contains(index) ? 0x805588bb : 0x40ffffff);
          if (index == hover && !values.get(index).isEmpty()) {
            try { tip = ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, values.get(index)).tooltip(); }
            catch (IllegalArgumentException unavailable) { tip = "Unavailable item: " + values.get(index); }
          }
        }
        int foldY = gridTop() + 8 * 18 - gridScroll;
        if (foldY >= gridTop() && foldY < gridBottom()) fill(gridLeft(), foldY - 1, gridLeft() + 144, foldY, 0xffffcc66);
        if (dragging && movingCells && dragCell >= 0 && hover >= 0) {
          int deltaCells = hover - dragCell;
          for (int source : dragSelection) {
            int target = source + deltaCells;
            if (target >= 0 && target < values.size()) {
              int x = gridLeft() + target % 8 * 18, y = gridTop() + target / 8 * 18 - gridScroll;
              fill(x + 1, y + 1, x + 17, y + 17, 0x80ffcc66);
            }
          }
        }
        unclip(); gridBar.render(this, gridTrack());
        button("Add row", gridLeft(), rowControlsY(), 70, 18, mx, my, values.size() < CreativeTabsData.MAX_ROWS * 8);
        button("Remove row", gridLeft() + 74, rowControlsY(), 70, 18, mx, my, values.size() > 8);
      }
      if (data.size() == 0) text("Add a tab. Search stays available.", left() + listWidth(), 88, 0xaaaaaa);
    }
    if (!problem.isEmpty()) text(fit(problem, span()), left(), 84, 0xff8888);
    if (!error.isEmpty()) text(fit(error, span()), left(), height - 69, 0xff8888);
    fill(0, height - 34, width, height, 0x70000000);
    button("Reset", footerX(0), height - 28, footerWidth(), 20, mx, my, editable());
    button("Cancel", footerX(1), height - 28, footerWidth(), 20, mx, my, true);
    button("Undo", footerX(2), height - 28, footerWidth(), 20, mx, my, editable() && (history.available() || pendingTitle()));
    button("Save", footerX(3), height - 28, footerWidth(), 20, mx, my, editable());
    if (dragging && movingCells && dragCell >= 0 && dragSource != null) {
      org.lwjgl.opengl.GL11.glPushMatrix(); org.lwjgl.opengl.GL11.glTranslatef(0, 0, 300);
      try { drawItem(dragSource.get(dragCell), pointerX - 8, pointerY - 8); }
      finally { org.lwjgl.opengl.GL11.glPopMatrix(); }
    } else if (!tip.isEmpty() && !exit.active() && !nativeGroups && !clearingCells) {
      foregroundTooltip(tip, pointerX, pointerY);
    }
    if (nativeGroups) {
      org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT | org.lwjgl.opengl.GL11.GL_CURRENT_BIT);
      try {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
        WaypointSetDropdown.render(this, nativeLabels(), -1, nativeFocus, 0,
            fillGroupX(), 52, fillGroupWidth(180), nativeLabels().size(), pointerX, pointerY);
      } finally { org.lwjgl.opengl.GL11.glPopAttrib(); }
    }
    exit.render(this, "Save changes to Creative tabs?", pointerX, pointerY);
  }
}
