package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import local.luke.power.map.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class WaypointsScreen extends UiScreen {
  private final Screen parent;
  private final TextInput search = new TextInput("", 128);
  private String selected = "", error = "";
  private int scroll, sort;
  private boolean confirmDelete, selectingSet, creatingSet;
  private final TextInput newSet = new TextInput("", 48);
  private final ScrollBar bar = new ScrollBar();
  private int setScroll, setIndex;
  private String newSetError = "";
  private List<Waypoint> cachedRows = List.of();
  private long cachedRevision = Long.MIN_VALUE;
  private WaypointStore cachedStore;
  private String cachedQuery = "", cachedSet = "";
  private int cachedSort = -1;
  private boolean cachedAll;
  private long cachedX, cachedZ;

  public WaypointsScreen(Screen parent) {
    this.parent = parent;
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
    TextDrag.cancel();
    bar.release();
  }

  private WaypointListLayout layout() { return WaypointListLayout.at(width, height); }
  private int left() { return layout().left(); }
  private int span() { return layout().width(); }
  private int rowBottom() { return layout().listBottom(); }
  private int cell() { return layout().actionWidth(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(layout().right() - 4, 72, rowBottom() - 72, rows().size() * 24, scroll); }
  private boolean modal() { return confirmDelete || selectingSet || creatingSet; }
  private void selectSet(String set) {
    Maps.waypoints().select(set);
    scroll = 0;
    selected = "";
    selectingSet = false;
  }
  private void beginNewSet() {
    selectingSet = false;
    creatingSet = true;
    search.focused = false;
    TextDrag.cancel();
    bar.release();
    newSet.setText("");
    newSet.focused = true;
    newSetError = "";
  }
  private void createSet() {
    try {
      String name = newSet.text().trim();
      Waypoint.text(name, 48);
      Maps.waypoints().createSet(name);
      scroll = 0;
      selected = "";
      creatingSet = false;
      newSet.focused = false;
      TextDrag.cancel();
    } catch (IllegalArgumentException failure) { newSetError = failure.getMessage(); }
  }

  private List<Waypoint> rows() {
    var store = Maps.waypoints();
    if (store == null || !store.ready()) {
      cachedRevision = Long.MIN_VALUE;
      return List.of();
    }
    String query = search.text().toLowerCase(Locale.ROOT);
    long px = (long) Math.floor(Maps.x()), pz = (long) Math.floor(Maps.z());
    if (cachedStore == store
        && cachedRevision == store.revision()
        && cachedQuery.equals(query)
        && cachedSort == sort
        && cachedSet.equals(store.selectedSet())
        && cachedAll == store.allSets()
        && (sort != 1 || cachedX == px && cachedZ == pz)) return cachedRows;
    cachedStore = store;
    cachedRevision = store.revision();
    cachedQuery = query;
    cachedSort = sort;
    cachedSet = store.selectedSet();
    cachedAll = store.allSets();
    cachedX = px;
    cachedZ = pz;
    Comparator<Waypoint> order =
        sort == 0
            ? Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT))
            : sort == 1
                ? Comparator.comparingDouble(p -> p.distance(Maps.x(), Maps.z()))
                : Comparator.comparingLong((Waypoint p) -> p.created).reversed();
    cachedRows =
        store.points().stream()
            .filter(
                p ->
                    (store.allSets() || p.set.equals(store.selectedSet()))
                        && (p.name + " " + p.set + " " + p.coordinates())
                            .toLowerCase(Locale.ROOT)
                            .contains(query))
            .sorted(order)
            .toList();
    return cachedRows;
  }

  private Waypoint selected() {
    return rows().stream().filter(p -> p.id.equals(selected)).findFirst().orElse(null);
  }

  protected void keyPressed(char c, int key) {
    if (creatingSet) {
      if (key == Keyboard.KEY_ESCAPE) { creatingSet = false; newSet.focused = false; TextDrag.cancel(); }
      else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); createSet(); }
      else { newSet.key(c, key); newSetError = ""; }
      return;
    }
    if (selectingSet) {
      var sets = Maps.waypoints().sets();
      if (key == Keyboard.KEY_ESCAPE) selectingSet = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        setIndex = Math.floorMod(setIndex + (key == Keyboard.KEY_UP ? -1 : 1), sets.size());
        int visible = layout().setRows(height, sets.size());
        setScroll = Math.max(0, Math.min(setIndex, Math.max(setScroll, setIndex - visible + 1)));
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
        click(); selectSet(sets.get(setIndex));
      }
      return;
    }
    if (confirmDelete) {
      if (key == Keyboard.KEY_ESCAPE) confirmDelete = false;
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) { minecraft.setScreen(parent); return; }
    search.key(c, key);
    scroll = 0;
  }

  protected void mouseClicked(int x, int y, int button) {
    if (confirmDelete) {
      if (button == 0 && inside(x, y, width / 2 - 82, height / 2 + 8, 80, 20)) {
        click();
        Maps.waypoints().remove(selected);
        selected = "";
        confirmDelete = false;
      } else if (button == 0 && inside(x, y, width / 2 + 2, height / 2 + 8, 80, 20)) {
        click();
        confirmDelete = false;
      }
      return;
    }
    if (creatingSet) {
      if (button != 0) return;
      int dialogWidth = Math.min(300, width - 24), dialogLeft = (width - dialogWidth) / 2;
      newSet.focused = inside(x, y, dialogLeft + 10, height / 2 - 10, dialogWidth - 20, 18);
      newSet.click(x, button);
      if (inside(x, y, dialogLeft + 10, height / 2 + 34, (dialogWidth - 24) / 2, 20)) { click(); createSet(); }
      else if (inside(x, y, width / 2 + 2, height / 2 + 34, (dialogWidth - 24) / 2, 20)) {
        click(); creatingSet = false; newSet.focused = false; TextDrag.cancel();
      }
      return;
    }
    if (button != 0 && button != 1) return;
    var store = Maps.waypoints();
    if (selectingSet) {
      if (store == null || !store.ready()) { selectingSet = false; return; }
      var sets = store.sets();
      int count = layout().setRows(height, sets.size());
      if (button == 0 && inside(x, y, left(), 69, layout().setWidth(), count * 20)) {
        int index = setScroll + (y - 69) / 20;
        if (index < sets.size()) { click(); selectSet(sets.get(index)); }
      } else selectingSet = false;
      return;
    }
    if (button == 0) {
      search.focused = inside(x, y, left(), 27, layout().searchWidth(), 18);
      search.click(x, button);
    }
    if (store == null || !store.ready()) return;
    if (inside(x, y, layout().sortX(), 27, layout().sortWidth(), 18)) {
      click(); sort = Math.floorMod(sort + (button == 1 ? -1 : 1), 3); return;
    }
    if (inside(x, y, layout().filterX(), 27, layout().filterWidth(), 18)) {
      click(); store.allSets(!store.allSets()); scroll = 0; return;
    }
    if (inside(x, y, layout().createX(), 49, layout().createWidth(), 18) && button == 0) {
      click(); beginNewSet(); return;
    }
    if (inside(x, y, left(), 49, layout().setWidth(), 18)) {
      click();
      var sets = store.sets();
      if (button == 1) selectSet(sets.get(Math.floorMod(sets.indexOf(store.selectedSet()) - 1, sets.size())));
      else {
        selectingSet = true; search.focused = false; TextDrag.cancel(); bar.release();
        setIndex = Math.max(0, sets.indexOf(store.selectedSet()));
        setScroll = Math.max(0, Math.min(setIndex, sets.size() - layout().setRows(height, sets.size())));
      }
      return;
    }
    if (button == 0 && bar.press(track(), x, y)) { scroll = (int)bar.drag(track(), y, true); return; }
    if (inside(x, y, left(), 72, span() - 8, rowBottom() - 72)) {
      int index = (y - 72 + scroll) / 24;
      var rows = rows();
      if (index < rows.size()) {
        Waypoint point = rows.get(index);
        selected = point.id;
        if (x < left() + 24) { click(); point.enabled = !point.enabled; store.put(point); }
        else if (button == 0) click();
      }
      return;
    }
    if (button != 0) return;
    int action = -1;
    for (int i = 0; i < 4; i++)
      if (inside(x, y, left() + i * (cell() + 4), height - 49, cell(), 18)) action = i;
    Waypoint point = selected();
    if (action == 0) {
      click();
      minecraft.setScreen(new WaypointEditorScreen(this, MapKeys.newWaypoint()));
    } else if (action == 1 && point != null) {
      click();
      minecraft.setScreen(new WaypointEditorScreen(this, point));
    } else if (action == 2 && point != null) {
      click();
      confirmDelete = true;
    } else if (action == 3 && point != null) {
      click();
      try {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(
                new StringSelection(
                    point.name
                        + ": "
                        + point.coordinates()
                        + " (dimension "
                        + point.dimension
                        + ")"),
                null);
      } catch (RuntimeException e) {
        error = "Clipboard is unavailable";
      }
    }
    if (inside(x, y, left(), height - 26, span() / 2 - 2, 20)
        && point != null
        && Maps.canTeleport(minecraft, point)) {
      click();
      Maps.teleport(minecraft, point);
      minecraft.setScreen(null);
    }
    if (inside(x, y, left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20)) {
      click();
      minecraft.setScreen(parent);
    }
  }

  public void onMouseEvent() {
    int wheel = Mouse.getEventDWheel();
    if (selectingSet && wheel != 0 && Maps.waypoints() != null) {
      int count = Maps.waypoints().sets().size();
      setScroll = Math.max(0, Math.min(count - layout().setRows(height, count), setScroll - Integer.signum(wheel) * 3));
    } else if (!modal() && wheel != 0) {
      scroll = (int)Math.max(0, Math.min(track().maximum(), scroll - Integer.signum(wheel) * 48));
    }
    super.onMouseEvent();
  }

  public void render(int mx, int my, float delta) {
    renderBackground();
    titleBar("Waypoints");
    var store = Maps.waypoints();
    int hoverX = modal() ? -1000 : mx, hoverY = modal() ? -1000 : my;
    input(search, left(), 27, layout().searchWidth(), hoverX, hoverY, "Search waypoints");
    button(new String[] {"Name", "Distance", "Newest"}[sort], layout().sortX(), 27, layout().sortWidth(), 18, hoverX, hoverY, true);
    button(store != null && store.allSets() ? "All sets" : "Active set", layout().filterX(), 27, layout().filterWidth(), 18,
        hoverX, hoverY, store != null && store.ready());
    button("", left(), 49, layout().setWidth(), 18, hoverX, hoverY, store != null && store.ready());
    text(fit("Set: " + (store == null ? "Loading" : store.selectedSet()), layout().setWidth() - 26), left() + 5, 54, 0xdddddd);
    chevron(left() + layout().setWidth() - 13, 56, selectingSet, 0xffdddddd);
    button("New set...", layout().createX(), 49, layout().createWidth(), 18, hoverX, hoverY, store != null && store.ready());
    scroll = (int)bar.drag(track(), my, !modal() && Mouse.isButtonDown(0));
    scroll = (int)Math.max(0, Math.min(track().maximum(), scroll));
    var rows = rows();
    clip(left(), 72, span(), Math.max(0, rowBottom() - 72));
    for (int i = Math.max(0, scroll / 24);
        i < rows.size() && 72 + i * 24 - scroll < rowBottom();
        i++) {
      Waypoint p = rows.get(i);
      int y = 72 + i * 24 - scroll;
      if (p.id.equals(selected)) fill(left(), y, left() + span() - 8, y + 23, 0x80557788);
      if (inside(hoverX, hoverY, left(), y, span() - 8, 24)) fill(left(), y, left() + span() - 8, y + 23, 0x60000000);
      button(p.enabled ? "+" : "-", left(), y + 2, 20, 18, hoverX, hoverY, true);
      text(
          fit(p.name, Math.max(20, span() / 2 - 30)),
          left() + 27,
          y + 3,
          p.enabled ? p.colour : 0x777777);
      text(
          fit(
              p.coordinates()
                  + " · "
                  + MapDimensions.name(p.dimension)
                  + " · "
                  + Math.round(p.distance(Maps.x(), Maps.z()))
                  + "m",
              span() / 2 - 8),
          left() + span() / 2,
          y + 3,
          0xcccccc);
      text(
          fit(p.set + (p.global ? " · Always show" : ""), span() - 38),
          left() + 27,
          y + 13,
          0x999999);
    }
    unclip();
    bar.render(this, track());
    if (rows.isEmpty())
      text(
          store != null && store.ready()
              ? "No matching waypoints"
              : "Waypoints are loading or protected",
          left() + 4,
          80,
          0xaaaaaa);
    String[] actions = {"New", "Edit", "Delete", "Copy"};
    Waypoint p = selected();
    for (int i = 0; i < 4; i++)
      button(
          actions[i],
          left() + i * (cell() + 4),
          height - 49,
          cell(),
          18,
          hoverX,
          hoverY,
          store != null && store.ready() && (i == 0 || p != null));
    button(
        "Teleport",
        left(),
        height - 26,
        span() / 2 - 2,
        20,
        hoverX,
        hoverY,
        p != null && Maps.canTeleport(minecraft, p));
    button("Back", left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20, hoverX, hoverY, true);
    if (!error.isEmpty() && !modal()) foregroundTooltip(error, mx, my);
    if (selectingSet && store != null && store.ready()) {
      var sets = store.sets(); int count = layout().setRows(height, sets.size());
      int bottom = 69 + count * 20;
      fill(left() - 1, 68, left() + layout().setWidth() + 1, bottom + 1, 0xff888888);
      fill(left(), 69, left() + layout().setWidth(), bottom, 0xff252525);
      for (int i = 0; i < count && i + setScroll < sets.size(); i++) {
        int index = i + setScroll, y = 69 + i * 20;
        boolean hovered = inside(mx, my, left(), y, layout().setWidth(), 20);
        if (hovered || index == setIndex) fill(left(), y, left() + layout().setWidth(), y + 20, 0x90000000);
        text(fit(sets.get(index), layout().setWidth() - 12), left() + 6, y + 6,
            sets.get(index).equals(store.selectedSet()) ? 0xffffff : 0xcccccc);
      }
    }
    if (creatingSet) {
      fill(0, 0, width, height, 0xc0000000);
      int dialogWidth = Math.min(300, width - 24), dialogLeft = (width - dialogWidth) / 2;
      fill(dialogLeft, height / 2 - 36, dialogLeft + dialogWidth, height / 2 + 64, 0xff252525);
      centeredText("New waypoint set", height / 2 - 28, 0xffffff);
      input(newSet, dialogLeft + 10, height / 2 - 10, dialogWidth - 20, mx, my, "Set name");
      if (!newSetError.isEmpty()) text(fit(newSetError, dialogWidth - 20), dialogLeft + 10, height / 2 + 14, 0xff8888);
      button("Create", dialogLeft + 10, height / 2 + 34, (dialogWidth - 24) / 2, 20, mx, my, true);
      button("Cancel", width / 2 + 2, height / 2 + 34, (dialogWidth - 24) / 2, 20, mx, my, true);
    }
    if (confirmDelete) {
      fill(0, 0, width, height, 0xc0000000);
      centeredText("Delete selected waypoint?", height / 2 - 14, 0xffffff);
      button("Delete", width / 2 - 82, height / 2 + 8, 80, 20, mx, my, true);
      button("Cancel", width / 2 + 2, height / 2 + 8, 80, 20, mx, my, true);
    }
  }

  public boolean shouldPause() {
    return MapConfig.current().pauseMap;
  }
}
