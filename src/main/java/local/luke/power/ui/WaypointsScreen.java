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
  private boolean confirmDelete;
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
  }

  private int left() {
    return 8;
  }

  private int span() {
    return width - 16;
  }

  private int rowBottom() {
    return height - 55;
  }

  private int cell() {
    return (span() - 12) / 4;
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
    if (confirmDelete) {
      if (key == Keyboard.KEY_ESCAPE) confirmDelete = false;
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) {
      minecraft.setScreen(parent);
      return;
    }
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
    if (button != 0) return;
    search.focused = inside(x, y, left(), 27, Math.max(40, span() - 206), 18);
    search.click(x, button);
    var store = Maps.waypoints();
    if (store == null || !store.ready()) return;
    if (inside(x, y, width - 206, 27, 92, 18)) {
      click();
      sort = (sort + 1) % 3;
      return;
    }
    if (inside(x, y, width - 110, 27, 102, 18)) {
      click();
      store.allSets(!store.allSets());
      return;
    }
    if (inside(x, y, left(), 49, span(), 18)) {
      click();
      var sets = store.sets();
      store.select(sets.get((sets.indexOf(store.selectedSet()) + 1) % sets.size()));
      scroll = 0;
      return;
    }
    if (y >= 72 && y < rowBottom()) {
      int index = (y - 72 + scroll) / 24;
      var rows = rows();
      if (index < rows.size()) {
        Waypoint point = rows.get(index);
        selected = point.id;
        if (x < left() + 24) {
          click();
          point.enabled = !point.enabled;
          store.put(point);
        } else if (button == 0) click();
      }
      return;
    }
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
    if (!confirmDelete && Mouse.getEventDWheel() != 0)
      scroll =
          Math.max(
              0,
              Math.min(
                  Math.max(0, rows().size() * 24 - (rowBottom() - 72)),
                  scroll - Integer.signum(Mouse.getEventDWheel()) * 48));
    super.onMouseEvent();
  }

  public void render(int mx, int my, float delta) {
    renderBackground();
    titleBar("Waypoints");
    var store = Maps.waypoints();
    input(search, left(), 27, Math.max(40, span() - 206), mx, my, "Search waypoints");
    button(
        new String[] {"Name", "Distance", "Newest"}[sort], width - 206, 27, 92, 18, mx, my, true);
    button(
        store != null && store.allSets() ? "All sets" : "Active set",
        width - 110,
        27,
        102,
        18,
        mx,
        my,
        store != null && store.ready());
    button(
        "Set: " + (store == null ? "Loading" : store.selectedSet()),
        left(),
        49,
        span(),
        18,
        mx,
        my,
        store != null && store.ready());
    var rows = rows();
    clip(left(), 72, span(), Math.max(0, rowBottom() - 72));
    for (int i = Math.max(0, scroll / 24);
        i < rows.size() && 72 + i * 24 - scroll < rowBottom();
        i++) {
      Waypoint p = rows.get(i);
      int y = 72 + i * 24 - scroll;
      if (p.id.equals(selected)) fill(left(), y, left() + span(), y + 23, 0x80557788);
      button(p.enabled ? "+" : "-", left(), y + 2, 20, 18, mx, my, true);
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
              span() / 2),
          left() + span() / 2,
          y + 3,
          0xcccccc);
      text(
          fit(p.set + (p.global ? " · Always show" : ""), span() - 30),
          left() + 27,
          y + 13,
          0x999999);
    }
    unclip();
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
          mx,
          my,
          store != null && store.ready() && (i == 0 || p != null));
    button(
        "Teleport",
        left(),
        height - 26,
        span() / 2 - 2,
        20,
        mx,
        my,
        p != null && Maps.canTeleport(minecraft, p));
    button("Back", left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20, mx, my, true);
    if (!error.isEmpty()) foregroundTooltip(error, mx, my);
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
