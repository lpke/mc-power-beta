package local.luke.power.ui;

import java.util.*;
import local.luke.power.map.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Waypoint fields remain private until Done, including changes from the nested colour picker. */
public final class WaypointEditorScreen extends UiScreen {
  private final Screen parent;
  private final Waypoint initial;
  private final List<TextInput> fields;
  private final DraftExitPrompt exit = new DraftExitPrompt();
  private final String[] labels = {"Name", "Set", "X", "Y", "Z", "Dimension", "Colour"};
  private boolean enabled, global;
  private String error = "";
  private int scroll;

  public WaypointEditorScreen(Screen parent, Waypoint waypoint) {
    this.parent = parent;
    initial = waypoint.copy();
    enabled = waypoint.enabled;
    global = waypoint.global;
    fields =
        List.of(
            new TextInput(waypoint.name, 64),
            new TextInput(waypoint.set, 48),
            new TextInput("" + waypoint.x, 10),
            new TextInput("" + waypoint.y, 3),
            new TextInput("" + waypoint.z, 10),
            new TextInput("" + waypoint.dimension, 5),
            new TextInput(String.format(Locale.ROOT, "#%06X", waypoint.colour), 7));
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    Keyboard.enableRepeatEvents(false);
    TextDrag.cancel();
  }

  private int left() {
    return (width - span()) / 2;
  }

  private int span() {
    return Math.min(360, width - 20);
  }

  private int row(int i) {
    return 32 + i * 25 - scroll;
  }

  private int dimensionWidth() {
    return Math.max(68, (span() - 96) / 2);
  }

  private int fieldLeft(int i) {
    return left() + 72 + (i == 5 ? dimensionWidth() + 4 : 0);
  }

  private int fieldWidth(int i) {
    return span() - 96 - (i == 5 ? dimensionWidth() + 4 : 0);
  }

  private String dimensionName() {
    try {
      return MapDimensions.name(Integer.parseInt(fields.get(5).text().trim()));
    } catch (NumberFormatException e) {
      return "Dimension";
    }
  }

  private Waypoint value() {
    Waypoint point = initial.copy();
    point.name = fields.get(0).text().trim();
    point.set = fields.get(1).text().trim();
    point.x = Integer.parseInt(fields.get(2).text().trim());
    point.y = Integer.parseInt(fields.get(3).text().trim());
    point.z = Integer.parseInt(fields.get(4).text().trim());
    point.dimension = Integer.parseInt(fields.get(5).text().trim());
    point.colour = local.luke.power.light.LightSettings.rgb(fields.get(6).text());
    point.enabled = enabled;
    point.global = global;
    point.validate();
    return point;
  }

  private boolean changed() {
    try {
      return !local.luke.power.config.Catalog.JSON
          .toJson(initial)
          .equals(local.luke.power.config.Catalog.JSON.toJson(value()));
    } catch (RuntimeException e) {
      return true;
    }
  }

  private void leave() {
    exit.leave(changed(), () -> minecraft.setScreen(parent));
  }

  private void save() {
    try {
      Maps.waypoints().put(value());
      minecraft.setScreen(parent);
    } catch (RuntimeException e) {
      error =
          e instanceof NumberFormatException ? "Enter whole-number coordinates" : e.getMessage();
    }
  }

  protected void keyPressed(char c, int key) {
    if (exit.key(key)) return;
    if (key == Keyboard.KEY_ESCAPE) {
      leave();
      return;
    }
    if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
      click();
      save();
      return;
    }
    if (key == Keyboard.KEY_TAB) {
      int index = 0;
      for (int i = 0; i < fields.size(); i++)
        if (fields.get(i).focused) {
          index = (i + 1) % fields.size();
          break;
        }
      for (var field : fields) field.focused = false;
      fields.get(index).focused = true;
      return;
    }
    for (var field : fields) field.key(c, key);
  }

  protected void mouseClicked(int x, int y, int button) {
    if (exit.click(this, x, y, button, this::save, () -> minecraft.setScreen(parent))) return;
    if (button != 0) return;
    if (inside(x, y, left(), height - 26, span() / 2 - 2, 20)) {
      click();
      save();
      return;
    }
    if (inside(x, y, left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20)) {
      click();
      leave();
      return;
    }
    if (y < 25 || y >= height - 43) return;
    for (int i = 0; i < fields.size(); i++) {
      var field = fields.get(i);
      field.focused = inside(x, y, fieldLeft(i), row(i), fieldWidth(i), 18);
      field.click(x, button);
      if (inside(x, y, left() + span() - 20, row(i), 20, 18)) {
        click();
        String[] reset = {
          "Waypoint",
          "Default",
          "" + (int) Math.floor(Maps.x()),
          "" + (int) Math.floor(Maps.y()),
          "" + (int) Math.floor(Maps.z()),
          "" + Maps.dimension(),
          "#FFCC33"
        };
        field.setText(reset[i]);
      }
    }
    if (inside(x, y, left() + 72, row(5), dimensionWidth(), 18)) {
      click();
      fields.get(5).setText(fields.get(5).text().equals("0") ? "-1" : "0");
    }
    if (inside(x, y, left() + 72, row(7), span() - 72, 18)) {
      click();
      enabled = !enabled;
    }
    if (inside(x, y, left() + 72, row(8), span() - 72, 18)) {
      click();
      global = !global;
    }
    if (inside(x, y, left() + 45, row(6), 22, 18)) {
      try {
        var setting =
            new local.luke.power.config.Setting(
                "waypoint.colour",
                "maps",
                "Map",
                "Waypoints",
                "Colour",
                "",
                local.luke.power.config.Setting.Kind.TEXT,
                new com.google.gson.JsonPrimitive(fields.get(6).text()),
                new com.google.gson.JsonPrimitive("#FFCC33"),
                0,
                0,
                1,
                List.of(),
                false);
        minecraft.setScreen(
            new ColourScreen(
                this, setting, () -> fields.get(6).setText(setting.value.getAsString())));
      } catch (RuntimeException e) {
        error = e.getMessage();
      }
    }
  }

  public void onMouseEvent() {
    if (!exit.active() && Mouse.getEventDWheel() != 0)
      scroll =
          Math.max(
              0,
              Math.min(
                  Math.max(0, 32 + 9 * 25 - (height - 43)),
                  scroll - Integer.signum(Mouse.getEventDWheel()) * 25));
    super.onMouseEvent();
  }

  public void render(int mx, int my, float delta) {
    renderBackground();
    titleBar("Waypoint");
    clip(left() - 2, 26, span() + 4, Math.max(0, height - 69));
    for (int i = 0; i < fields.size(); i++) {
      text(labels[i], left(), row(i) + 5, 0xcccccc);
      input(fields.get(i), fieldLeft(i), row(i), fieldWidth(i), mx, my, "");
      if (i == 5)
        button(
            fit(dimensionName(), dimensionWidth() - 6),
            left() + 72,
            row(i),
            dimensionWidth(),
            18,
            mx,
            my,
            true);
      button("R", left() + span() - 20, row(i), 20, 18, mx, my, true);
    }
    try {
      colourButton(fields.get(6).text(), left() + 45, row(6), 22, mx, my);
    } catch (RuntimeException ignored) {
    }
    text("Visible", left(), row(7) + 5, 0xcccccc);
    button(enabled ? "On" : "Off", left() + 72, row(7), span() - 72, 18, mx, my, true);
    text("Always show", left(), row(8) + 5, 0xcccccc);
    button(global ? "On" : "Off", left() + 72, row(8), span() - 72, 18, mx, my, true);
    unclip();
    if (!error.isEmpty()) text(fit(error, width - 20), 10, height - 40, 0xff7777);
    button(
        "Done",
        left(),
        height - 26,
        span() / 2 - 2,
        20,
        mx,
        my,
        Maps.waypoints() != null && Maps.waypoints().ready());
    button("Cancel", left() + span() / 2 + 2, height - 26, span() / 2 - 2, 20, mx, my, true);
    exit.render(this, "Save waypoint changes?", mx, my);
  }

  public boolean shouldPause() {
    return MapConfig.current().pauseMap;
  }
}
