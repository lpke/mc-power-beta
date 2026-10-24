package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import local.luke.power.map.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Waypoint actions use the row's context menu; creation and Done share the footer. */
public final class WaypointsScreen extends UiScreen {
  private static final int ROW = 28, EYE = 18, MENU_ROW = 18;
  private final Screen parent;
  private final TextInput search = new TextInput("", 128);
  private final ScrollBar bar = new ScrollBar();
  private final WaypointSetDialog setDialog = new WaypointSetDialog();
  private final WaypointRowClicks rowClicks = new WaypointRowClicks();
  private String selected = "", error = "";
  private int scroll, sort, setScroll, setIndex;
  private boolean selectingSet, creatingSet, context, moving;
  private int contextX, contextY, contextScroll;
  private long deleteUntil;
  private String deletingSet = "";
  private WaypointStore deletingStore;
  private long deletingRevision;
  private int deletingCount;
  private List<Waypoint> cachedRows = List.of();
  private long cachedRevision = Long.MIN_VALUE;
  private WaypointStore cachedStore;
  private String cachedQuery = "", cachedSet = "";
  private int cachedSort = -1;
  private boolean cachedAll;
  private long cachedX, cachedZ;
  private WaypointStore moveStore;
  private long moveRevision = Long.MIN_VALUE;
  private String moveFrom = "";
  private List<String> cachedMoveSets = List.of();

  public WaypointsScreen(Screen parent) { this.parent = parent; }
  public void init() { Keyboard.enableRepeatEvents(true); rowClicks.clear(); }
  public void tick() { if (!Display.isActive()) rowClicks.clear(); }
  public void removed() { Keyboard.enableRepeatEvents(false); TextDrag.cancel(); bar.release(); rowClicks.clear(); }
  private WaypointListLayout layout() { return WaypointListLayout.at(width, height); }
  private int left() { return layout().left(); }
  private int span() { return layout().width(); }
  private int rowBottom() { return layout().listBottom(); }
  private ScrollBar.Track track() { return new ScrollBar.Track(layout().right() - 4, 72, rowBottom() - 72, rows().size() * ROW, scroll); }
  private boolean modal() { return selectingSet || creatingSet || context || !deletingSet.isEmpty(); }
  private void selectSet(String set) {
    Maps.waypoints().select(set); scroll = 0; selected = ""; selectingSet = false;
  }
  private void beginNewSet() {
    selectingSet = false; creatingSet = true; search.focused = false; TextDrag.cancel(); bar.release();
    setDialog.open(name -> { Maps.waypoints().createSet(name); scroll = 0; selected = ""; });
  }
  private void beginDeleteSet() {
    var store = Maps.waypoints();
    if (store == null || !store.ready() || store.selectedSet().equalsIgnoreCase("Default")) return;
    deletingSet = store.selectedSet(); deletingStore = store; deletingRevision = store.revision();
    deletingCount = (int)store.points().stream().filter(point -> point.set.equals(deletingSet)).count();
    selectingSet = false; search.focused = false; TextDrag.cancel(); bar.release();
  }
  private void deleteSet() {
    var store = Maps.waypoints();
    if (store != deletingStore || !store.ready() || store.revision() != deletingRevision) {
      error = "Waypoint sets changed. Review the set and try again.";
    } else {
      try { store.deleteSet(deletingSet); selected = ""; scroll = 0; error = ""; }
      catch (IllegalArgumentException | IllegalStateException failure) { error = failure.getMessage(); }
    }
    deletingSet = ""; deletingStore = null;
  }
  private List<Waypoint> rows() {
    var store = Maps.waypoints();
    if (store == null || !store.ready()) { cachedRevision = Long.MIN_VALUE; return List.of(); }
    String query = search.text().toLowerCase(Locale.ROOT);
    long px = (long)Math.floor(Maps.x()), pz = (long)Math.floor(Maps.z());
    if (cachedStore == store && cachedRevision == store.revision() && cachedQuery.equals(query) && cachedSort == sort
        && cachedSet.equals(store.selectedSet()) && cachedAll == store.allSets() && (sort != 1 || cachedX == px && cachedZ == pz)) return cachedRows;
    cachedStore = store; cachedRevision = store.revision(); cachedQuery = query; cachedSort = sort;
    cachedSet = store.selectedSet(); cachedAll = store.allSets(); cachedX = px; cachedZ = pz;
    Comparator<Waypoint> order = sort == 0 ? Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT))
        : sort == 1 ? Comparator.comparingDouble(p -> p.distance(Maps.x(), Maps.z()))
        : Comparator.comparingLong((Waypoint p) -> p.created).reversed();
    var points = store.points();
    var scores = new HashMap<String, Integer>();
    for (Waypoint point : points) scores.put(point.id, WaypointSearch.score(query, point));
    cachedRows = points.stream().filter(p -> (store.allSets() || p.set.equals(store.selectedSet()))
        && scores.get(p.id) != FuzzySearch.NONE)
        .sorted(Comparator.comparingInt((Waypoint point) -> scores.get(point.id)).thenComparing(order).thenComparing(p -> p.id)).toList();
    return cachedRows;
  }
  private Waypoint selected() { return rows().stream().filter(p -> p.id.equals(selected)).findFirst().orElse(null); }
  private Waypoint rowAt(int x, int y) {
    if (!inside(x, y, left(), 72, span() - 8, rowBottom() - 72)) return null;
    int index = (y - 72 + scroll) / ROW;
    var points = rows();
    return index < points.size() ? points.get(index) : null;
  }
  private boolean selectedAt(int x, int y) { var point = rowAt(x, y); return point != null && point.id.equals(selected); }
  private boolean eyeAt(int x, int y) {
    int rowY = 72 + (y - 72 + scroll) / ROW * ROW - scroll;
    return inside(x, y, left() + 6, rowY + 5, EYE, EYE);
  }
  private void rowClick(Waypoint point, long eventNanos) {
    click();
    if (rowClicks.press(point.id, eventNanos)) minecraft.setScreen(new WaypointEditorScreen(this, point));
  }
  private List<String> moveSets() {
    Waypoint point = selected();
    if (point == null) return List.of();
    var store = Maps.waypoints();
    if (moveStore != store || moveRevision != store.revision() || !moveFrom.equals(point.set)) {
      moveStore = store; moveRevision = store.revision(); moveFrom = point.set;
      cachedMoveSets = store.sets().stream().filter(set -> !set.equals(point.set)).toList();
    }
    return cachedMoveSets;
  }
  private List<String> contextActions() {
    if (moving) return moveSets();
    Waypoint point = selected(); if (point == null) return List.of();
    var actions = new ArrayList<String>(); actions.add("Edit waypoint");
    actions.add(System.nanoTime() < deleteUntil ? "Confirm delete?" : "Delete waypoint");
    actions.add("Copy coordinates");
    if (Maps.canTeleport(minecraft, point)) actions.add("Teleport");
    if (MapConfig.current().worldMap) actions.add("Center here");
    actions.add("Set destination");
    if (!moveSets().isEmpty()) actions.add("Move to");
    return actions;
  }
  private int menuWidth() { return Math.min(180, width - 16); }
  private int menuRows() { return Math.min(contextActions().size(), Math.max(1, (height - 16) / MENU_ROW)); }
  private void fitMenu() {
    fitMenu(contextActions());
  }
  private int menuRows(List<String> actions) { return Math.min(actions.size(), Math.max(1, (height - 16) / MENU_ROW)); }
  private void fitMenu(List<String> actions) {
    contextScroll = Math.max(0, Math.min(contextScroll, actions.size() - menuRows(actions)));
    contextX = Math.max(8, Math.min(width - menuWidth() - 8, contextX));
    contextY = Math.max(8, Math.min(height - menuRows(actions) * MENU_ROW - 8, contextY));
  }
  private void contextAction(String action) {
    Waypoint point = selected(); if (point == null) { context = false; return; }
    if (moving) {
      Maps.waypoints().move(point.id, action); context = moving = false; return;
    }
    if (action.equals("Move to")) { moving = true; contextScroll = 0; fitMenu(); return; }
    if (action.equals("Delete waypoint")) { deleteUntil = System.nanoTime() + 4_000_000_000L; return; }
    context = false;
    switch (action) {
      case "Edit waypoint" -> minecraft.setScreen(new WaypointEditorScreen(this, point));
      case "Confirm delete?" -> { Maps.waypoints().remove(point.id); selected = ""; }
      case "Copy coordinates" -> {
        try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(point.coordinates()), null); }
        catch (RuntimeException failure) { error = "Clipboard is unavailable"; }
      }
      case "Teleport" -> { if (Maps.canTeleport(minecraft, point)) { Maps.teleport(minecraft, point); minecraft.setScreen(null); } }
      case "Center here" -> minecraft.setScreen(new WorldMapScreen(this, point));
      case "Set destination" -> { Maps.destination(point); minecraft.setScreen(null); }
    }
  }
  protected void keyPressed(char c, int key) {
    rowClicks.clear();
    if (!deletingSet.isEmpty()) {
      if (key == Keyboard.KEY_ESCAPE) { deletingSet = ""; deletingStore = null; }
      return;
    }
    if (setDialog.key(this, c, key)) { creatingSet = setDialog.active(); return; }
    if (context) {
      if (key == Keyboard.KEY_ESCAPE) {
        if (moving) { moving = false; contextScroll = 0; fitMenu(); }
        else context = false;
      }
      return;
    }
    if (selectingSet) {
      var sets = Maps.waypoints().sets();
      if (key == Keyboard.KEY_ESCAPE) selectingSet = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        setIndex = Math.floorMod(setIndex + (key == Keyboard.KEY_UP ? -1 : 1), sets.size());
        int visible = layout().setRows(height, sets.size());
        setScroll = Math.max(0, Math.min(setIndex, Math.max(setScroll, setIndex - visible + 1)));
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); selectSet(sets.get(setIndex)); }
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) { minecraft.setScreen(parent); return; }
    search.key(c, key); scroll = 0;
  }
  protected void mouseClicked(int x, int y, int button) {
    if (modal() || button != 0 || rowAt(x, y) == null || eyeAt(x, y)) rowClicks.clear();
    if (!deletingSet.isEmpty()) {
      if (button == 0 && inside(x, y, width / 2 - 112, height / 2 + 20, 108, 20)) { click(); deleteSet(); }
      else if (button == 0 && inside(x, y, width / 2 + 4, height / 2 + 20, 108, 20)) { click(); deletingSet = ""; deletingStore = null; }
      return;
    }
    if (setDialog.press(this, x, y, button)) { creatingSet = setDialog.active(); return; }
    if (button != 0 && button != 1) return;
    if (context) {
      fitMenu();
      if (button == 0 && inside(x, y, contextX, contextY, menuWidth(), menuRows() * MENU_ROW)) {
        click(); contextAction(contextActions().get(contextScroll + (y - contextY) / MENU_ROW));
      } else {
        context = moving = false;
        if (!selectedAt(x, y)) selected = "";
      }
      return;
    }
    if (!selectedAt(x, y)) selected = "";
    var store = Maps.waypoints();
    if (selectingSet) {
      if (store == null || !store.ready()) { selectingSet = false; return; }
      var sets = store.sets(); int count = layout().setRows(height, sets.size());
      if (button == 0 && inside(x, y, left(), 69, layout().setWidth(), count * 20)) {
        int index = setScroll + (y - 69) / 20; if (index < sets.size()) { click(); selectSet(sets.get(index)); }
      } else selectingSet = false;
      return;
    }
    if (button == 0 && inside(x, y, layout().doneX(), height - 26, layout().footerWidth(), 20)) { click(); minecraft.setScreen(parent); return; }
    if (inside(x, y, left(), 27, layout().searchWidth(), 18)) {
      search.focused = true; String previous = search.text(); search.click(x, button);
      if (!previous.equals(search.text())) scroll = 0;
      return;
    }
    if (button == 0) search.focused = false;
    if (store == null || !store.ready()) return;
    if (button == 0 && inside(x, y, layout().newX(), height - 26, layout().footerWidth(), 20)) { click(); minecraft.setScreen(new WaypointEditorScreen(this, MapKeys.newWaypoint())); return; }
    if (inside(x, y, layout().sortX(), 27, layout().sortWidth(), 18)) { click(); sort = Math.floorMod(sort + (button == 1 ? -1 : 1), 3); return; }
    if (inside(x, y, layout().filterX(), 27, layout().filterWidth(), 18)) { click(); store.allSets(!store.allSets()); scroll = 0; return; }
    if (inside(x, y, layout().createX(), 49, layout().createWidth(), 18) && button == 0) { click(); beginNewSet(); return; }
    if (inside(x, y, layout().deleteX(), 49, layout().deleteWidth(), 18) && button == 0) {
      if (!store.selectedSet().equalsIgnoreCase("Default")) { click(); beginDeleteSet(); }
      return;
    }
    if (inside(x, y, left(), 49, layout().setWidth(), 18)) {
      click(); var sets = store.sets();
      if (button == 1) selectSet(sets.get(Math.floorMod(sets.indexOf(store.selectedSet()) - 1, sets.size())));
      else { selectingSet = true; search.focused = false; TextDrag.cancel(); bar.release();
        setIndex = Math.max(0, sets.indexOf(store.selectedSet()));
        setScroll = Math.max(0, Math.min(setIndex, sets.size() - layout().setRows(height, sets.size()))); }
      return;
    }
    if (button == 0 && bar.press(track(), x, y)) { scroll = (int)bar.drag(track(), y, true); return; }
    if (inside(x, y, left(), 72, span() - 8, rowBottom() - 72)) {
      int index = (y - 72 + scroll) / ROW; var rows = rows();
      if (index < rows.size()) {
        Waypoint point = rows.get(index); selected = point.id;
        if (button == 0 && eyeAt(x, y)) { click(); point.enabled = !point.enabled; store.put(point); }
        else if (button == 1) {
          click(); context = true; moving = false; deleteUntil = 0; contextScroll = 0; contextX = x; contextY = y;
          search.focused = false; TextDrag.cancel(); bar.release(); fitMenu();
        } else rowClick(point, Mouse.getEventNanoseconds());
      }
    }
  }
  public void onMouseEvent() {
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0) rowClicks.clear();
    if (context && wheel != 0) { contextScroll -= Integer.signum(wheel) * 3; fitMenu(); }
    else if (selectingSet && wheel != 0 && Maps.waypoints() != null) {
      int count = Maps.waypoints().sets().size(); setScroll = Math.max(0, Math.min(count - layout().setRows(height, count), setScroll - Integer.signum(wheel) * 3));
    } else if (!modal() && wheel != 0) scroll = (int)Math.max(0, Math.min(track().maximum(), scroll - Integer.signum(wheel) * 48));
    super.onMouseEvent();
  }
  protected void mouseReleased(int x, int y, int button) {
    if (button == 0) { rowClicks.release(); bar.release(); }
    super.mouseReleased(x, y, button);
  }
  public void render(int mx, int my, float delta) {
    renderBackground(); titleBar("Waypoints");
    var store = Maps.waypoints(); boolean ready = store != null && store.ready();
    int hoverX = modal() ? -1000 : mx, hoverY = modal() ? -1000 : my;
    input(search, left(), 27, layout().searchWidth(), hoverX, hoverY, "Search waypoints");
    button("Sort: " + new String[]{"Name", "Distance", "Newest"}[sort], layout().sortX(), 27, layout().sortWidth(), 18, hoverX, hoverY, ready);
    button(ready && store.allSets() ? "All sets" : "This set", layout().filterX(), 27, layout().filterWidth(), 18, hoverX, hoverY, ready);
    WaypointSetDropdown.button(this, "Set: " + (store == null ? "Default" : store.selectedSet()), left(), 49, layout().setWidth(), hoverX, hoverY, ready, selectingSet);
    button("Delete set", layout().deleteX(), 49, layout().deleteWidth(), 18, hoverX, hoverY, ready && !store.selectedSet().equalsIgnoreCase("Default"));
    button("New set", layout().createX(), 49, layout().createWidth(), 18, hoverX, hoverY, ready);
    List<Waypoint> rows = rows(); scroll = (int)Math.max(0, Math.min(track().maximum(), bar.drag(track(), my, Mouse.isButtonDown(0))));
    clip(left(), 72, span(), Math.max(0, rowBottom() - 72));
    for (int i = 0; i < rows.size(); i++) {
      int y = 72 + i * ROW - scroll; if (y + ROW <= 72 || y >= rowBottom()) continue;
      Waypoint p = rows.get(i);
      if (p.id.equals(selected)) fill(left() + 2, y + 1, left() + span() - 8, y + ROW - 1, 0x604e7294);
      else if (inside(hoverX, hoverY, left(), y, span() - 8, ROW)) fill(left() + 2, y + 1, left() + span() - 8, y + ROW - 1, 0x90000000);
      iconButton(p.enabled ? "eye" : "eyeClosed", left() + 6, y + 5, EYE, EYE, hoverX, hoverY, true, p.enabled);
      String details = fit("§f" + p.coordinates() + " §7· " + MapDimensions.name(p.dimension) + " · " + Math.round(p.distance(Maps.x(), Maps.z())) + "m", span() / 2 - 12);
      int detailsX = layout().right() - 12 - textRenderer.getWidth(details);
      int nameWidth = Math.max(20, detailsX - left() - 38);
      text(fit(p.name, nameWidth), left() + 30, y + 4, p.enabled ? p.colour : 0x777777);
      text(details, detailsX, y + (ROW - 8) / 2, 0xeeeeee);
      text(fit(p.set + (p.global ? " · Always show" : ""), nameWidth), left() + 30, y + 15, 0x999999);
    }
    unclip(); bar.render(this, track());
    if (rows.isEmpty()) text(ready ? "No matching waypoints" : "Waypoints are loading or protected", left() + 4, 80, 0xaaaaaa);
    button("New waypoint", layout().newX(), height - 26, layout().footerWidth(), 20, hoverX, hoverY, ready);
    button("Done", layout().doneX(), height - 26, layout().footerWidth(), 20, hoverX, hoverY, true);
    if (!error.isEmpty()) tooltip(error, left(), height - 40);
    if (selectingSet && ready) {
      var sets = store.sets(); int count = layout().setRows(height, sets.size());
      WaypointSetDropdown.render(this, sets, sets.indexOf(store.selectedSet()), setIndex, setScroll,
          left(), 69, layout().setWidth(), count, mx, my);
    }
    if (context) {
      var actions = contextActions(); fitMenu(actions); int count = menuRows(actions);
      fill(contextX - 1, contextY - 1, contextX + menuWidth() + 1, contextY + count * MENU_ROW + 1, 0xffaaaaaa);
      for (int i = 0; i < count; i++) {
        int y = contextY + i * MENU_ROW; String label = actions.get(i + contextScroll);
        fill(contextX, y, contextX + menuWidth(), y + MENU_ROW, inside(mx, my, contextX, y, menuWidth(), MENU_ROW) ? 0xff454545 : 0xff252525);
        text(fit(label, menuWidth() - 20), contextX + 6, y + 5, 0xffffff);
        if (!moving && label.equals("Move to")) chevron(contextX + menuWidth() - 11, y + 7, false, 0xffcccccc);
      }
    }
    setDialog.render(this, mx, my);
    if (!deletingSet.isEmpty()) {
      fill(0, 0, width, height, 0xc0000000);
      centeredText(fit("Delete set \"" + deletingSet + "\"?", width - 24), height / 2 - 26, 0xffffff);
      centeredText(deletingCount == 0 ? "This set is empty." : deletingCount + (deletingCount == 1 ? " waypoint will move to Default." : " waypoints will move to Default."), height / 2 - 10, 0xcccccc);
      button("Delete set", width / 2 - 112, height / 2 + 20, 108, 20, mx, my, true);
      button("Cancel", width / 2 + 4, height / 2 + 20, 108, 20, mx, my, true);
    } else if (!modal() && ready && store.selectedSet().equalsIgnoreCase("Default")
        && inside(mx, my, layout().deleteX(), 49, layout().deleteWidth(), 18)) tooltip("The Default set cannot be deleted.", mx, my);
  }
  public boolean shouldPause() { return MapConfig.current().pauseMap || parent != null && parent.shouldPause(); }
}
