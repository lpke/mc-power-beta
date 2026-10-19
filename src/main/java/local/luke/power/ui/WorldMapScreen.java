package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import local.luke.power.map.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Full map navigation only changes the view. Panning never loads native terrain. */
public final class WorldMapScreen extends UiScreen {
  private final MapMath.View view = new MapMath.View(Maps.x(), Maps.z());
  private final MapZoom zoom = new MapZoom();
  private final TextInput serverMap = new TextInput(Maps.submap(), 48);
  private int dimension = Maps.dimension(), layer = Maps.layer();
  private boolean dragging, context;
  private int lastX, lastY, contextX, contextY;
  private Waypoint target;
  private String message = "";
  private MapSettings styleSource, worldStyle;

  private MapSettings worldStyle() {
    if (styleSource != MapConfig.current()) {
      styleSource = MapConfig.current();
      worldStyle = MapConfig.copy();
      worldStyle.opacity = 1;
    }
    return worldStyle;
  }

  private int top() {
    return 0;
  }

  private int bottom() {
    return height;
  }

  private MapProjection projection() {
    return new MapProjection(
        view.x, view.z, view.scale, 0, top(), width, Math.max(1, bottom() - top()), 0);
  }

  private static MapProjection.Point bounded(MapProjection.Point p) {
    return new MapProjection.Point(
        MapMath.bounded(p.x(), -MapMath.BORDER, MapMath.BORDER),
        MapMath.bounded(p.y(), -MapMath.BORDER, MapMath.BORDER),
        0,
        0);
  }

  private int buttonWidth() {
    return Math.min(90, (width - 28) / 6);
  }

  private boolean overControls(int x, int y) {
    if (inside(x, y, width - 124, 4, 116, 18)) return true;
    if (minecraft.world != null
        && minecraft.world.isRemote
        && inside(x, y, 8, 4, Math.min(180, width / 3), 18)) return true;
    for (int i = 0; i < 6; i++)
      if (inside(x, y, 8 + i * (buttonWidth() + 2), height - 24, buttonWidth(), 18)) return true;
    return false;
  }

  private void overlay(String label, int x, int y, int colour) {
    fill(x - 3, y - 3, x + textRenderer.getWidth(label) + 3, y + 11, 0x90000000);
    text(label, x, y, colour);
  }

  private void centre() {
    zoom.cancel();
    view.x = Maps.x();
    view.z = Maps.z();
    dimension = Maps.dimension();
    layer = Maps.layer();
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
  }

  public void removed() {
    dragging = false;
    zoom.cancel();
    Keyboard.enableRepeatEvents(false);
    TextDrag.cancel();
  }

  protected void keyPressed(char c, int key) {
    if (context) {
      if (key == Keyboard.KEY_ESCAPE) context = false;
      return;
    }
    if (serverMap.focused) {
      if (key == Keyboard.KEY_RETURN) {
        try {
          Maps.submap(serverMap.text().trim());
          message = "Map selected";
        } catch (RuntimeException e) {
          message = e.getMessage();
        }
        serverMap.focused = false;
        return;
      }
      if (key == Keyboard.KEY_ESCAPE) {
        serverMap.focused = false;
        serverMap.setText(Maps.submap());
        return;
      }
      serverMap.key(c, key);
      return;
    }
    if (key == Keyboard.KEY_ESCAPE || local.luke.power.input.Bindings.matches(MapKeys.MAP, key)) {
      minecraft.setScreen(null);
      return;
    }
    if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_SPACE) centre();
    if (key == Keyboard.KEY_UP) view.pan(0, 32);
    if (key == Keyboard.KEY_DOWN) view.pan(0, -32);
    if (key == Keyboard.KEY_LEFT) view.pan(32, 0);
    if (key == Keyboard.KEY_RIGHT) view.pan(-32, 0);
    if (key == Keyboard.KEY_EQUALS || key == Keyboard.KEY_ADD)
      zoom.wheel(view, 1, 0, 0, System.nanoTime());
    if (key == Keyboard.KEY_MINUS || key == Keyboard.KEY_SUBTRACT)
      zoom.wheel(view, -1, 0, 0, System.nanoTime());
    if (local.luke.power.input.Bindings.matches(MapKeys.WAYPOINT, key)) createAt(view.x, view.z);
    if (key == Keyboard.KEY_G) goTo();
  }

  private void goTo() {
    minecraft.setScreen(
        new MapCoordinatesScreen(
            this,
            view.x,
            view.z,
            (x, z) -> {
              view.x = x;
              view.z = z;
            }));
  }

  private void createAt(double x, double z) {
    Waypoint p = MapKeys.newWaypoint();
    p.x = (int) Math.floor(x);
    p.z = (int) Math.floor(z);
    p.dimension = dimension;
    MapTile tile =
        Maps.cache() == null ? null : Maps.cache().peek(MapMath.key(dimension, layer, 0, p.x, p.z));
    if (tile != null && tile.known(MapMath.index(p.x, p.z)))
      p.y = Math.min(127, tile.height(MapMath.index(p.x, p.z)) + 1);
    minecraft.setScreen(new WaypointEditorScreen(this, p));
  }

  protected void mouseClicked(int x, int y, int button) {
    zoom.cancel();
    if (context) {
      if (button == 0 && inside(x, y, contextX, contextY, 142, 90)) {
        int action = (y - contextY) / 18;
        context = false;
        click();
        if (action == 0) minecraft.setScreen(new WaypointEditorScreen(this, target));
        if (action == 1)
          try {
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(target.coordinates()), null);
            message = "Coordinates copied";
          } catch (RuntimeException e) {
            message = "Clipboard is unavailable";
          }
        if (action == 2 && Maps.canTeleport(minecraft, target)) {
          Maps.teleport(minecraft, target);
          minecraft.setScreen(null);
        }
        if (action == 4) {
          Maps.destination(target);
          minecraft.setScreen(null);
        }
        if (action == 3) {
          view.x = target.x + .5;
          view.z = target.z + .5;
        }
      } else context = false;
      return;
    }
    if (button == 0) {
      if (inside(x, y, width - 64, 4, 56, 18)) {
        click();
        minecraft.setScreen(null);
        return;
      }
      if (inside(x, y, width - 124, 4, 56, 18)) {
        click();
        goTo();
        return;
      }
      for (int i = 0; i < 6; i++)
        if (inside(x, y, 8 + i * (buttonWidth() + 2), height - 24, buttonWidth(), 18)) {
          click();
          switch (i) {
            case 0 -> centre();
            case 1 -> minecraft.setScreen(new WaypointsScreen(this));
            case 2 -> PowerOptionsScreen.openMap(minecraft, this);
            case 3 -> {
              layer = layer < 0 ? Math.max(0, Math.min(15, (int) Maps.y() / 8)) : -1;
            }
            case 4 -> {
              dimension = dimension == 0 ? -1 : 0;
            }
            case 5 -> {
              if (Maps.cache() != null)
                MapExport.start(
                    Maps.cache(),
                    projection(),
                    dimension,
                    layer,
                    MapConfig.copy(),
                    minecraft.world.isRemote ? 0 : minecraft.world.getSeed(),
                    !minecraft.world.isRemote);
            }
          }
          return;
        }
      if (minecraft.world.isRemote && inside(x, y, 8, 4, Math.min(180, width / 3), 18)) {
        serverMap.focused = true;
        serverMap.click(x, button);
        return;
      }
      serverMap.focused = false;
    }
    if (y < top() || y >= bottom() || overControls(x, y)) return;
    if (button == 0) {
      dragging = true;
      lastX = x;
      lastY = y;
    }
    if (button == 1) {
      var position = bounded(projection().world(x, y));
      target = MapKeys.newWaypoint();
      target.x = (int) Math.floor(position.x());
      target.z = (int) Math.floor(position.y());
      target.dimension = dimension;
      MapTile tile = Maps.cache().peek(MapMath.key(dimension, layer, 0, target.x, target.z));
      if (tile != null && tile.known(MapMath.index(target.x, target.z)))
        target.y = Math.min(127, tile.height(MapMath.index(target.x, target.z)) + 1);
      for (Waypoint point : Maps.points())
        if (point.dimension == dimension) {
          var screen = projection().screen(point.x + .5, point.z + .5);
          if (Math.hypot(screen.x() - x, screen.y() - y) <= 7) {
            target = point.copy();
            break;
          }
        }
      dragging = false;
      context = true;
      contextX = Math.max(0, Math.min(width - 142, x));
      contextY = Math.max(top(), Math.min(bottom() - 90, y));
    }
  }

  protected void mouseReleased(int x, int y, int button) {
    if (button == 0) dragging = false;
  }

  public void onMouseEvent() {
    int x = Mouse.getEventX() * width / minecraft.displayWidth,
        y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if (!Display.isActive()) dragging = false;
    if (dragging && !context) {
      zoom.cancel();
      view.pan(x - lastX, y - lastY);
      lastX = x;
      lastY = y;
    }
    if (!context
        && !overControls(x, y)
        && Mouse.getEventDWheel() != 0
        && y >= top()
        && y < bottom())
      zoom.wheel(
          view,
          Mouse.getEventDWheel(),
          x - width / 2d,
          y - (top() + bottom()) / 2d,
          System.nanoTime());
    super.onMouseEvent();
  }

  public void render(int mx, int my, float delta) {
    zoom.update(view, System.nanoTime());
    if (!Display.isActive()) dragging = false;
    fill(0, 0, width, height, 0xff101015);
    MapRenderer.beginFrame(delta);
    if (Maps.cache() != null && minecraft.world != null) {
      var projection = projection();
      var style = worldStyle();
      MapRenderer.terrain(
          Maps.cache(),
          projection,
          dimension,
          layer,
          false,
          style,
          !minecraft.world.isRemote,
          minecraft.world.getSeed());
      GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
      clip(0, top(), width, bottom() - top());
      MapRenderer.markers(minecraft, projection, dimension, false, false);
      GL11.glPopAttrib();
    }
    int headingX =
        minecraft.world != null && minecraft.world.isRemote ? Math.min(180, width / 3) + 16 : 8;
    overlay(
        fit(
            "World map · " + (layer < 0 ? "Surface" : "Cave Y " + (layer * 8 + 7)),
            Math.max(10, Math.min(180, width - 132 - headingX))),
        headingX,
        9,
        0xffffff);
    button("Go to", width - 124, 4, 56, 18, mx, my, true);
    button("Close", width - 64, 4, 56, 18, mx, my, true);
    if (minecraft.world != null && minecraft.world.isRemote)
      input(serverMap, 8, 4, Math.min(180, width / 3), mx, my, "Map name");
    var cursor = bounded(projection().world(mx, my));
    String readout = (int) Math.floor(cursor.x()) + ", " + (int) Math.floor(cursor.y());
    if (Maps.cache() != null) {
      MapTile tile =
          Maps.cache()
              .peek(
                  MapMath.key(
                      dimension,
                      layer,
                      0,
                      (int) Math.floor(cursor.x()),
                      (int) Math.floor(cursor.y())));
      if (tile != null) {
        int i = MapMath.index((int) Math.floor(cursor.x()), (int) Math.floor(cursor.y()));
        if (tile.known(i))
          readout += " · Y " + tile.height(i) + " · " + MapPalette.biome(tile.biome(i));
      }
    }
    int blocks = (int) Math.max(1, Math.round(80 / view.scale));
    boolean separateReadout = width < 640 || minecraft.world != null && minecraft.world.isRemote;
    int readoutY = separateReadout ? 29 : 9;
    int readoutWidth = separateReadout ? width - 16 : Math.max(40, width - 520);
    readout = fit(readout, readoutWidth);
    overlay(readout, (width - textRenderer.getWidth(readout)) / 2, readoutY, 0xffffff);
    String hint =
        width >= 600
            ? "Drag to pan · Scroll to zoom · Right-click for waypoints"
            : "Drag · Scroll · Right-click";
    overlay(fit(hint, width - 112), 8, height - 40, 0xcccccc);
    int barX = width - 88, barY = height - 31;
    fill(barX - 3, height - 47, width - 5, height - 27, 0x90000000);
    fill(barX, barY, barX + 80, barY + 1, 0xffcccccc);
    fill(barX, barY - 2, barX + 1, barY + 2, 0xffcccccc);
    fill(barX + 79, barY - 2, barX + 80, barY + 2, 0xffcccccc);
    String scaleLabel = blocks + " blocks";
    text(scaleLabel, barX + (80 - textRenderer.getWidth(scaleLabel)) / 2, height - 44, 0xcccccc);
    String[] labels = {
      "Centre",
      "Waypoints",
      "Settings",
      layer < 0 ? "Caves" : "Surface",
      MapDimensions.name(dimension),
      "Export"
    };
    for (int i = 0; i < 6; i++)
      button(
          fit(labels[i], buttonWidth() - 6),
          8 + i * (buttonWidth() + 2),
          height - 24,
          buttonWidth(),
          18,
          mx,
          my,
          true);
    if (context) {
      fill(contextX, contextY, contextX + 142, contextY + 90, 0xf0202020);
      String[] options = {
        Maps.waypoints() != null && Maps.waypoints().contains(target.id)
            ? "Edit waypoint"
            : "Create waypoint",
        "Copy coordinates",
        "Teleport",
        "Centre here",
        "Set destination"
      };
      for (int i = 0; i < 5; i++) {
        if (inside(mx, my, contextX, contextY + i * 18, 142, 18))
          fill(contextX, contextY + i * 18, contextX + 142, contextY + (i + 1) * 18, 0xff444444);
        text(
            options[i],
            contextX + 5,
            contextY + i * 18 + 5,
            i == 2 && !Maps.canTeleport(minecraft, target) ? 0x666666 : 0xffffff);
      }
    }
    String status = MapExport.status();
    if (status.isEmpty()) status = message.isEmpty() ? Maps.error() : message;
    if (!status.isEmpty()) overlay(fit(status, width - 16), 8, readoutY + 20, 0xffdd99);
  }

  public boolean shouldPause() {
    return MapConfig.current().pauseMap;
  }
}
