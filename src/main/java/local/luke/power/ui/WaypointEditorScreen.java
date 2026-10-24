package local.luke.power.ui;

import java.util.*;
import local.luke.power.map.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** The point, abbreviation and newly named sets stay private until Done. */
public final class WaypointEditorScreen extends UiScreen {
  private static final int ABBREVIATION = 7, VISIBLE = 8, ALWAYS = 9, ROWS = 10;
  private final Screen parent;
  private final Waypoint initial;
  private final List<TextInput> fields;
  private final local.luke.power.config.Setting savedColors;
  private final com.google.gson.JsonElement originalColors;
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final WaypointSetDialog setDialog = new WaypointSetDialog();
  private final List<String> draftSets = new ArrayList<>();
  private final String[] labels = {"Name", "Set", "X", "Y", "Z", "Dimension", "Color", "Abbreviation"};
  private boolean enabled, global, selectingSet;
  private String error = "";
  private int scroll, setScroll, setIndex;

  public WaypointEditorScreen(Screen parent, Waypoint waypoint) {
    this.parent = parent; initial = waypoint.copy(); enabled = waypoint.enabled; global = waypoint.global;
    originalColors = local.luke.power.config.Catalog.JSON.toJsonTree(MenuPreferences.current().colours);
    savedColors = new local.luke.power.config.Setting("interface.colours", "interface", "Interface", "", "Saved colors", "",
        local.luke.power.config.Setting.Kind.LIST, originalColors, new com.google.gson.JsonArray(), 0, 16, 1, List.of(), false);
    fields = List.of(new TextInput(waypoint.name, 64), new TextInput(waypoint.set, 48),
        new TextInput("" + waypoint.x, 24), new TextInput("" + waypoint.y, 24), new TextInput("" + waypoint.z, 24),
        new TextInput("" + waypoint.dimension, 5), new TextInput(String.format(Locale.ROOT, "#%06X", waypoint.colour), 7),
        new TextInput(waypoint.abbreviation == null ? "" : waypoint.abbreviation, 8));
  }
  public void init() { Keyboard.enableRepeatEvents(true); }
  public void removed() { Keyboard.enableRepeatEvents(false); TextDrag.cancel(); }
  private int left() { return (width - span()) / 2; }
  private int span() { return Math.min(360, width - 20); }
  private int row(int index) {
    int display = index == ABBREVIATION ? 1 : index > 0 && index < ABBREVIATION ? index + 1 : index;
    return 32 + display * 25 - scroll;
  }
  private int dimensionWidth() { return Math.max(68, (span() - 96) / 2); }
  private int fieldLeft(int index) { return left() + 72 + (index == 5 ? dimensionWidth() + 4 : 0); }
  private int fieldWidth(int index) { return span() - 96 - (index == 5 ? dimensionWidth() + 4 : 0); }
  private String dimensionName() {
    try { return MapDimensions.name(Integer.parseInt(fields.get(5).text().trim())); }
    catch (NumberFormatException failure) { return "Dimension"; }
  }
  private Waypoint value() {
    return value(WaypointCoordinates.current(minecraft));
  }
  private Waypoint value(local.luke.power.input.ReportedPosition.Pose origin) {
    Waypoint point = WaypointCoordinates.resolve(initial, fields.get(2).text().trim(), fields.get(3).text().trim(),
        fields.get(4).text().trim(), origin);
    point.name = fields.get(0).text().strip(); point.set = fields.get(1).text().strip();
    point.dimension = Integer.parseInt(fields.get(5).text().trim());
    point.colour = local.luke.power.light.LightSettings.rgb(fields.get(6).text());
    point.abbreviation = fields.get(ABBREVIATION).text().strip(); point.enabled = enabled; point.global = global;
    point.validate(); return point;
  }
  private boolean changed() {
    for (int i = 2; i <= 4; i++) if (fields.get(i).text().trim().startsWith("~")) return true;
    try {
      var origin = new local.luke.power.input.ReportedPosition.Pose(initial.x, initial.y, initial.z, 0, 0);
      return !savedColors.value.equals(originalColors) || !local.luke.power.config.Catalog.JSON.toJson(initial).equals(local.luke.power.config.Catalog.JSON.toJson(value(origin)));
    }
    catch (RuntimeException failure) { return true; }
  }
  private void leave() { exit.leave(changed(), () -> minecraft.setScreen(parent)); }
  private void save() {
    var store = Maps.waypoints();
    if (store == null || !store.ready()) { error = "Waypoints are loading or protected after a storage error."; return; }
    try {
      Waypoint point = value();
      store.validatePut(point);
      if (!savedColors.value.equals(originalColors)) {
        List<String> colors = new ArrayList<>(); savedColors.value.getAsJsonArray().forEach(c -> colors.add(c.getAsString()));
        MenuPreferences.update(preferences -> preferences.colours = colors);
      }
      store.put(point); minecraft.setScreen(parent);
    }
    catch (java.io.IOException | RuntimeException failure) { error = failure instanceof NumberFormatException ? "Enter whole-number coordinates" : failure.getMessage(); }
  }
  private List<String> sets() {
    var names = new TreeSet<String>(); var store = Maps.waypoints();
    if (store != null && store.ready()) names.addAll(store.sets());
    names.add("Default"); names.add(fields.get(1).text()); names.addAll(draftSets); return List.copyOf(names);
  }
  private int setTop() { return Math.max(26, Math.min(height - 43 - setRows() * 20, row(1) + 20)); }
  private int setRows() { return Math.min(sets().size() + 1, Math.max(1, (height - 69) / 20)); }
  private void chooseSet(int index) {
    var sets = sets(); selectingSet = false;
    if (index < sets.size()) { fields.get(1).setText(sets.get(index)); return; }
    setDialog.open(name -> {
      if (sets().stream().anyMatch(set -> set.equalsIgnoreCase(name))) throw new IllegalArgumentException("A set with that name already exists");
      draftSets.add(name); fields.get(1).setText(name);
    });
  }
  private void beginSet() {
    fields.forEach(field -> field.focused = false); TextDrag.cancel(); selectingSet = true;
    setIndex = Math.max(0, sets().indexOf(fields.get(1).text()));
    setScroll = Math.max(0, Math.min(setIndex, sets().size() + 1 - setRows()));
  }
  protected void keyPressed(char c, int key) {
    if (exit.key(key) || setDialog.key(this, c, key)) return;
    if (selectingSet) {
      if (key == Keyboard.KEY_ESCAPE) selectingSet = false;
      else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
        setIndex = Math.floorMod(setIndex + (key == Keyboard.KEY_UP ? -1 : 1), sets().size() + 1);
        setScroll = Math.max(0, Math.min(setIndex, Math.max(setScroll, setIndex - setRows() + 1)));
      } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); chooseSet(setIndex); }
      return;
    }
    if (key == Keyboard.KEY_ESCAPE) { leave(); return; }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { click(); save(); return; }
    if (key == Keyboard.KEY_TAB) {
      var editable = List.of(0, ABBREVIATION, 2, 3, 4, 5, 6);
      int index = editable.get(0);
      for (int i = 0; i < editable.size(); i++) if (fields.get(editable.get(i)).focused) {
        index = editable.get((i + 1) % editable.size()); break;
      }
      fields.forEach(field -> field.focused = false); fields.get(index).focused = true; return;
    }
    for (int i = 0; i < fields.size(); i++) if (i != 1) fields.get(i).key(c, key);
  }
  protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent)) || setDialog.press(this, x, y, button)) return;
    if (selectingSet) {
      if (button == 0 && inside(x, y, fieldLeft(1), setTop(), fieldWidth(1), setRows() * 20)) {
        click(); chooseSet(setScroll + (y - setTop()) / 20);
      } else selectingSet = false;
      return;
    }
    if (button == 1) {
      if (y < 26 || y >= height - 43) return;
      for (int i = 0; i < fields.size(); i++) {
        var field = fields.get(i);
        field.focused = i != 1 && inside(x, y, fieldLeft(i), row(i), fieldWidth(i), 18);
        field.click(x, button);
      }
      return;
    }
    if (button != 0) return;
    if (inside(x, y, left(), height - 26, span() / 2 - 2, 20)) { click(); save(); return; }
    if (inside(x, y, left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20)) { click(); leave(); return; }
    if (y < 26 || y >= height - 43) return;
    if (inside(x, y, fieldLeft(1), row(1), fieldWidth(1), 18)) { click(); beginSet(); return; }
    for (int i = 0; i < fields.size(); i++) {
      var field = fields.get(i); field.focused = i != 1 && inside(x, y, fieldLeft(i), row(i), fieldWidth(i), 18);
      if (i != 1) field.click(x, button);
      if (inside(x, y, left() + span() - 20, row(i), 20, 18)) {
        click();
        String[] reset = {"Waypoint", "Default", "" + (int)Math.floor(Maps.x()), "" + (int)Math.floor(Maps.y()),
            "" + (int)Math.floor(Maps.z()), "" + Maps.dimension(), "#FFCC33", ""};
        field.setText(reset[i]);
      }
    }
    if (inside(x, y, left() + 72, row(5), dimensionWidth(), 18)) { click(); fields.get(5).setText(fields.get(5).text().equals("0") ? "-1" : "0"); }
    if (inside(x, y, left() + 72, row(VISIBLE), span() - 72, 18)) { click(); enabled = !enabled; }
    if (inside(x, y, left() + 72, row(ALWAYS), span() - 72, 18)) { click(); global = !global; }
    if (inside(x, y, left() + 45, row(6), 22, 18)) {
      try {
        var setting = new local.luke.power.config.Setting("waypoint.colour", "maps", "Map", "Waypoints", "Color", "",
            local.luke.power.config.Setting.Kind.TEXT, new com.google.gson.JsonPrimitive(fields.get(6).text()),
            new com.google.gson.JsonPrimitive("#FFCC33"), 0, 0, 1, List.of(), false);
        minecraft.setScreen(new ColourScreen(this, setting, null, () -> fields.get(6).setText(setting.value.getAsString()), savedColors));
      } catch (RuntimeException failure) { error = failure.getMessage(); }
    }
  }
  public void onMouseEvent() {
    int wheel = Mouse.getEventDWheel();
    if (selectingSet && wheel != 0) setScroll = Math.max(0, Math.min(sets().size() + 1 - setRows(), setScroll - Integer.signum(wheel) * 3));
    else if (!exit.active() && !setDialog.active() && wheel != 0)
      scroll = Math.max(0, Math.min(Math.max(0, 32 + ROWS * 25 - (height - 43)), scroll - Integer.signum(wheel) * 25));
    super.onMouseEvent();
  }
  public void render(int mx, int my, float delta) {
    renderBackground(); titleBar("Waypoint");
    boolean modal = exit.active() || selectingSet || setDialog.active(); int hoverX = modal ? -1000 : mx, hoverY = modal ? -1000 : my;
    clip(left() - 2, 26, span() + 4, Math.max(0, height - 69));
    for (int i = 0; i < fields.size(); i++) {
      text(labels[i], left(), row(i) + 5, 0xcccccc);
      if (i == 1) {
        WaypointSetDropdown.button(this, fields.get(i).text(), fieldLeft(i), row(i), fieldWidth(i), hoverX, hoverY, true, selectingSet);
      } else input(fields.get(i), fieldLeft(i), row(i), fieldWidth(i), hoverX, hoverY, i == ABBREVIATION ? "Automatic" : "");
      if (i == 5) button(fit(dimensionName(), dimensionWidth() - 6), left() + 72, row(i), dimensionWidth(), 18, hoverX, hoverY, true);
      button("R", left() + span() - 20, row(i), 20, 18, hoverX, hoverY, true);
    }
    try { colourButton(fields.get(6).text(), left() + 45, row(6), 22, hoverX, hoverY); } catch (RuntimeException ignored) {}
    text("Visible", left(), row(VISIBLE) + 5, 0xcccccc); button(enabled ? "On" : "Off", left() + 72, row(VISIBLE), span() - 72, 18, hoverX, hoverY, true);
    text("Always show", left(), row(ALWAYS) + 5, 0xcccccc); button(global ? "On" : "Off", left() + 72, row(ALWAYS), span() - 72, 18, hoverX, hoverY, true);
    unclip();
    if (!error.isEmpty()) text(fit(error, width - 20), 10, height - 40, 0xff7777);
    else if (fields.subList(2, 5).stream().anyMatch(field -> field.focused))
      text(fit("Use ~ for current position, ~offset to add an offset.", width - 20), 10, height - 40, 0xaaaaaa);
    button("Done", left(), height - 26, span() / 2 - 2, 20, hoverX, hoverY, Maps.waypoints() != null && Maps.waypoints().ready());
    button("Cancel", left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20, hoverX, hoverY, true);
    if (selectingSet) {
      var sets = sets(); int left = fieldLeft(1), top = setTop(), width = fieldWidth(1);
      var labels = new ArrayList<>(sets); labels.add("New set...");
      WaypointSetDropdown.render(this, labels, sets.indexOf(fields.get(1).text()), setIndex, setScroll,
          left, top, width, setRows(), mx, my);
    }
    exit.render(this, "Save waypoint changes?", mx, my); setDialog.render(this, mx, my);
  }
  public boolean shouldPause() { return MapConfig.current().pauseMap || parent != null && parent.shouldPause(); }
}
