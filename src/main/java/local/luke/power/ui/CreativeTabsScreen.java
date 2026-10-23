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
  private boolean dragging, selectionPress;
  private List<String> dragSource;
  private Set<Integer> dragSelection;

  public CreativeTabsScreen(Screen parent, Setting setting, Runnable accepted) {
    this.parent = parent; this.setting = setting; this.accepted = accepted;
    try {
      data = CreativeTabsData.read(setting.value.getAsJsonObject());
      if (!data.customized()) data = CreativeTabsData.read(CreativeTabsBackend.defaultLayout());
      original = data.json(); select(0);
    } catch (RuntimeException e) { problem = Objects.toString(e.getMessage(), "Creative tabs are unavailable"); }
  }
  private int span() { return Math.min(470, width - 20); }
  private int left() { return (width - span()) / 2; }
  private int listWidth() { return Math.max(92, Math.min(160, span() - 180)); }
  private int gridLeft() { return left() + listWidth() + (span() - listWidth() - 144) / 2; }
  private int gridTop() { return 85; }
  private int gridBottom() { return height - 70; }
  private int listBottom() { return height - 72; }
  private int visibleListRows() { return Math.max(1, (listBottom() - 59) / 23); }
  private boolean editable() { return data != null && problem.isEmpty(); }
  private JsonObject tab() { return data.tab(selected); }
  private boolean hasTab() { return editable() && data.size() > 0; }
  private List<String> paddedSlots() {
    List<String> values = CreativeTabsData.slots(tab());
    int length = Math.max(64, ((values.size() + 7) / 8) * 8);
    while (values.size() < length) values.add("");
    return values;
  }
  private void storeSlots(List<String> values) { CreativeTabsData.slots(tab(), values); entries.clear(); }
  private void select(int index) {
    selected = Math.max(0, Math.min(index, data.size() - 1));
    cells.clear(); anchor = -1; gridScroll = 0; rename = false; title.focused = false; cancelDrag();
    if (data.size() > 0) title.setText(tab().get("title").getAsString());
  }
  private void cancelDrag() { dragCell = dragTab = -1; dragging = false; dragSource = null; dragSelection = null; }
  @Override public void init() { Keyboard.enableRepeatEvents(true); }
  @Override public void removed() { Keyboard.enableRepeatEvents(false); TextDrag.cancel(); cancelDrag(); }
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
    tab().addProperty("title", value); rename = false; title.focused = false; error = ""; return true;
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
      data.append(CreativeTabsData.tab(UUID.randomUUID().toString(), "New tab", "1", Collections.nCopies(64, "")));
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
    if (!editable() || !inside(x, y, left(), 59, listWidth() - 6, Math.max(0, listBottom() - 59))) return -1;
    int row = listScroll + (y - 59) / 23;
    return row < data.size() ? row : -1;
  }
  private boolean ctrl() { return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL); }
  private boolean shift() { return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT); }
  @Override protected void keyPressed(char character, int key) {
    cancelDrag();
    if (exit.key(key)) return;
    if (nativeGroups) {
      if (key == Keyboard.KEY_ESCAPE) nativeGroups = false;
      else if (key == Keyboard.KEY_UP) nativeFocus = Math.max(0, nativeFocus - 1);
      else if (key == Keyboard.KEY_DOWN) nativeFocus = Math.min(nativeLabels().size() - 1, nativeFocus + 1);
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); fillNative(nativeFocus); }
      return;
    }
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
    cancelDrag();
    int x = Mouse.getEventX() * width / minecraft.displayWidth;
    if (x < left() + listWidth()) listScroll = Math.max(0, Math.min(Math.max(0, data.size() - visibleListRows()), listScroll - Integer.signum(wheel)));
    else if (hasTab()) gridScroll = Math.max(0, Math.min(Math.max(0, paddedSlots().size() / 8 * 18 - Math.max(0, gridBottom() - gridTop())), gridScroll - Integer.signum(wheel) * 18));
  }
  @Override protected void mouseClicked(int x, int y, int button) {
    cancelDrag();
    if (exit.click(this, x, y, button, this::save, () -> showScreen(parent))) return;
    if (nativeGroups) {
      if (button != 0) return;
      for (int i = 0; i < nativeLabels().size(); i++)
        if (inside(x, y, left() + 52, 52 + i * 20, 180, 20)) { click(); fillNative(i); return; }
      nativeGroups = false; return;
    }
    if (rename) {
      int nameLeft = left() + listWidth() + 24, nameWidth = span() - listWidth() - 46;
      boolean nameHit = inside(x, y, nameLeft, 59, nameWidth, 18);
      boolean checkHit = inside(x, y, left() + span() - 18, 58, 18, 20);
      boolean footerHit = inside(x, y, width / 2 - 80, height - 28, 160, 20);
      if (!nameHit && !checkHit && !footerHit) cancelTitle();
    }
    if (button == 0 && inside(x, y, width / 2 + 4, height - 28, 76, 20)) { click(); leave(); return; }
    if (!editable()) return;
    if (button == 0 && inside(x, y, width / 2 - 80, height - 28, 76, 20)) { click(); save(); return; }
    if (button == 0 && inside(x, y, left(), 30, 48, 20)) { click(); addTab(); return; }
    if (button == 0 && hasTab() && inside(x, y, left() + 52, 30, 92, 20)) { click(); if (commitTitle()) { nativeGroupData(); nativeFocus = 0; nativeGroups = true; } return; }
    if (button == 0 && inside(x, y, left() + span() - 48, 30, 48, 20) && hasTab()) {
      click(); data.remove(selected); select(selected); listScroll = Math.min(listScroll, Math.max(0, data.size() - visibleListRows())); return;
    }
    if (button == 0 && inside(x, y, left(), height - 52, 48, 18)) {
      click(); data = CreativeTabsData.read(CreativeTabsBackend.defaultLayout()); select(0); listScroll = 0; return;
    }
    int row = tabRow(x, y);
    if (row >= 0 && button == 0) {
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
    if (button == 0 && rename && inside(x, y, left() + span() - 18, 58, 18, 20)) { click(); commitTitle(); return; }
    if (!commitTitle()) return;
    if (button == 0 && inside(x, y, panelLeft, 58, 20, 20)) { click(); picker(tab().get("icon").getAsString(), value -> tab().addProperty("icon", value)); return; }
    if (button == 0 && inside(x, y, gridLeft(), height - 65, 72, 18)) {
      List<String> values = paddedSlots();
      if (values.size() < CreativeTabsData.MAX_ROWS * 8) { click(); values.addAll(Collections.nCopies(8, "")); storeSlots(values); } return;
    }
    if (button == 0 && inside(x, y, gridLeft() + 76, height - 65, 68, 18) && !cells.isEmpty()) {
      click(); List<String> values = paddedSlots(); for (int index : cells) values.set(index, ""); storeSlots(values); return;
    }
    int cell = gridCell(x, y);
    if (cell < 0 || button != 0 && button != 1) return;
    if (button == 1) { List<String> values = paddedSlots(); values.set(cell, ""); storeSlots(values); return; }
    selectionPress = shift() || ctrl();
    if (shift() && anchor >= 0) {
      if (!ctrl()) cells.clear();
      for (int i = Math.min(anchor, cell); i <= Math.max(anchor, cell); i++) cells.add(i);
    } else if (ctrl()) { if (!cells.remove(cell)) cells.add(cell); anchor = cell; }
    else { if (!cells.contains(cell)) { cells.clear(); cells.add(cell); } anchor = cell; }
    dragCell = cell; pressX = x; pressY = y; dragging = false;
    dragSource = paddedSlots(); dragSelection = Set.copyOf(cells);
  }
  @Override protected void mouseReleased(int x, int y, int button) {
    if (button != 0) return;
    if (exit.active() || !Display.isActive()) { cancelDrag(); return; }
    dragging |= Math.abs(x - pressX) + Math.abs(y - pressY) >= 4;
    if (dragTab >= 0) {
      int destination = tabRow(x, y);
      if (dragging && destination >= 0) { data.move(dragTab, destination); selected = destination; }
      cancelDrag(); return;
    }
    if (dragCell >= 0) {
      int source = dragCell, destination = gridCell(x, y);
      if (dragging && destination >= 0 && dragSelection != null && !dragSelection.isEmpty()) {
        int delta = destination - source;
        try {
          storeSlots(CreativeTabsData.moveCells(dragSource, dragSelection, delta));
          cells.clear(); for (int value : dragSelection) cells.add(value + delta); anchor = destination;
        } catch (IllegalArgumentException e) { error = e.getMessage(); }
      } else if (!dragging && !selectionPress && destination == source) {
        List<String> values = paddedSlots();
        picker(values.get(source), value -> { List<String> next = paddedSlots(); next.set(source, value); storeSlots(next); });
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
  @Override public void render(int mx, int my, float delta) {
    int pointerX = mx, pointerY = my;
    if (!Display.isActive()) cancelDrag();
    if (dragCell >= 0 || dragTab >= 0) dragging |= Math.abs(mx - pressX) + Math.abs(my - pressY) >= 4;
    if (exit.active() || nativeGroups) { mx = -100; my = -100; }
    renderBackground(); titleBar("Creative tabs");
    button("Add", left(), 30, 48, 20, mx, my, editable() && data.size() < CreativeTabsData.MAX_TABS);
    WaypointSetDropdown.button(this, "Fill group", left() + 52, 30, 92, mx, my, hasTab(), nativeGroups);
    button("Remove", left() + span() - 48, 30, 48, 20, mx, my, hasTab());
    String tip = "";
    if (editable()) {
      clip(left(), 59, listWidth() - 6, Math.max(0, listBottom() - 59));
      for (int i = listScroll; i < Math.min(data.size(), listScroll + visibleListRows()); i++) {
        int y = 59 + (i - listScroll) * 23;
        boolean hover = tabRow(mx, my) == i;
        if (i == selected || hover) fill(left(), y, left() + listWidth() - 6, y + 21, i == selected ? 0x80445566 : 0x50333333);
        drawItem(data.tab(i).get("icon").getAsString(), left() + 2, y + 2);
        text(fit(data.tab(i).get("title").getAsString(), listWidth() - 30), left() + 22, y + 7, 0xeeeeee);
        if (dragging && dragTab >= 0 && hover) fill(left(), y, left() + listWidth() - 6, y + 1, 0xffffff88);
      }
      unclip();
      if (hasTab()) {
        int panelLeft = left() + listWidth(), nameWidth = span() - listWidth() - 46;
        button("", panelLeft, 58, 20, 20, mx, my, true); drawItem(tab().get("icon").getAsString(), panelLeft + 2, 60);
        if (rename) { input(title, panelLeft + 24, 59, nameWidth, mx, my, "Tab name"); iconButton("check", left() + span() - 18, 58, 18, 20, mx, my, true, false); }
        else { text(fit(tab().get("title").getAsString(), nameWidth + 18), panelLeft + 24, 65, 0xffffff); }
        List<String> values = paddedSlots();
        clip(gridLeft(), gridTop(), 144, Math.max(0, gridBottom() - gridTop()));
        int first = gridScroll / 18, last = Math.min(values.size() / 8, (gridScroll + Math.max(0, gridBottom() - gridTop()) + 17) / 18);
        int hover = gridCell(mx, my);
        for (int row = first; row < last; row++) for (int col = 0; col < 8; col++) {
          int index = row * 8 + col, x = gridLeft() + col * 18, y = gridTop() + row * 18 - gridScroll;
          fill(x, y, x + 18, y + 18, 0xff666666); fill(x + 1, y + 1, x + 17, y + 17, 0xff252525);
          drawItem(values.get(index), x + 1, y + 1);
          if (cells.contains(index) || index == hover) fill(x + 1, y + 1, x + 17, y + 17, cells.contains(index) ? 0x805588bb : 0x40ffffff);
          if (index == hover && !values.get(index).isEmpty()) {
            try { tip = ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER, values.get(index)).tooltip(); }
            catch (IllegalArgumentException unavailable) { tip = "Unavailable item: " + values.get(index); }
          }
        }
        int foldY = gridTop() + 8 * 18 - gridScroll;
        if (foldY >= gridTop() && foldY < gridBottom()) fill(gridLeft(), foldY - 1, gridLeft() + 144, foldY, 0xffffcc66);
        if (dragging && dragCell >= 0 && hover >= 0) {
          int deltaCells = hover - dragCell;
          for (int source : dragSelection) {
            int target = source + deltaCells;
            if (target >= 0 && target < values.size()) {
              int x = gridLeft() + target % 8 * 18, y = gridTop() + target / 8 * 18 - gridScroll;
              fill(x + 1, y + 1, x + 17, y + 17, 0x80ffcc66);
            }
          }
        }
        unclip();
        button("Add row", gridLeft(), height - 65, 72, 18, mx, my, values.size() < CreativeTabsData.MAX_ROWS * 8);
        button("Clear", gridLeft() + 76, height - 65, 68, 18, mx, my, !cells.isEmpty());
        if (inside(mx, my, gridLeft(), height - 45, 144, 12)) tip = "The gold line marks the first eight rows visible in Creative.\nScroll to edit later rows. Shift selects a range; Ctrl selects individual cells.\nDrag selected cells to move them. Right-click or Delete clears cells.";
        text("8 visible rows", gridLeft(), height - 44, 0xaaaaaa);
      }
      if (data.size() == 0) text("Add a tab. Search stays available.", left() + listWidth(), 88, 0xaaaaaa);
    }
    if (!problem.isEmpty()) text(fit(problem, span()), left(), 84, 0xff8888);
    if (!error.isEmpty()) text(fit(error, span()), left(), height - 82, 0xff8888);
    button("Reset", left(), height - 52, 48, 18, mx, my, editable());
    fill(0, height - 34, width, height, 0x70000000);
    button("Save", width / 2 - 80, height - 28, 76, 20, mx, my, editable());
    button("Cancel", width / 2 + 4, height - 28, 76, 20, mx, my, true);
    if (!tip.isEmpty() && !exit.active() && !nativeGroups) tooltip(tip, pointerX, pointerY);
    if (nativeGroups) WaypointSetDropdown.render(this, nativeLabels(), -1, nativeFocus, 0,
        left() + 52, 52, 180, nativeLabels().size(), pointerX, pointerY);
    exit.render(this, "Save changes to Creative tabs?", pointerX, pointerY);
  }
}
