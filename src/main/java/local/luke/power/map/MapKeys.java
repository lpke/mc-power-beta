package local.luke.power.map;

import java.util.List;
import local.luke.power.input.Bindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public final class MapKeys {
  public static final KeyBinding MAP = new KeyBinding("powerbeta.map", Keyboard.KEY_M),
      WAYPOINT = new KeyBinding("powerbeta.waypoint", Keyboard.KEY_B),
      WAYPOINTS = new KeyBinding("powerbeta.waypoints", Keyboard.KEY_U),
      ENLARGE = new KeyBinding("powerbeta.enlargeMap", Keyboard.KEY_Z),
      ZOOM_IN = new KeyBinding("powerbeta.mapZoomIn", Keyboard.KEY_I),
      ZOOM_OUT = new KeyBinding("powerbeta.mapZoomOut", Keyboard.KEY_O),
      TOGGLE = new KeyBinding("powerbeta.minimap", 0);

  public static List<KeyBinding> all() {
    return List.of(MAP, WAYPOINT, WAYPOINTS, ENLARGE, ZOOM_IN, ZOOM_OUT, TOGGLE);
  }

  public static boolean available(String id) {
    return switch (id) {
      case "powerbeta.map" -> MapConfig.current().worldMap;
      case "powerbeta.enlargeMap", "powerbeta.mapZoomIn", "powerbeta.mapZoomOut" ->
          MapConfig.current().minimap;
      default -> true;
    };
  }

  public static void press(Minecraft mc, int key) {
    if (mc.world == null
        || mc.player == null
        || mc.currentScreen != null
        || mc.paused
        || mc.player.dead
        || mc.player.health <= 0
        || !Display.isActive()) return;
    if (Bindings.matches(MAP, key) && MapConfig.current().worldMap)
      mc.setScreen(new local.luke.power.ui.WorldMapScreen());
    else if (Bindings.matches(WAYPOINT, key)
        && Maps.waypoints() != null
        && Maps.waypoints().ready())
      mc.setScreen(new local.luke.power.ui.WaypointEditorScreen(null, newWaypoint()));
    else if (Bindings.matches(WAYPOINTS, key) && Maps.waypoints() != null)
      mc.setScreen(new local.luke.power.ui.WaypointsScreen(null));
    else if (Bindings.matches(TOGGLE, key)) {
      var s = MapConfig.copy();
      s.minimap = !s.minimap;
      MapConfig.queued(s);
    } else if (Bindings.matches(ZOOM_IN, key) || Bindings.matches(ZOOM_OUT, key)) {
      var s = MapConfig.copy();
      s.zoom = MapMath.bounded(s.zoom * (Bindings.matches(ZOOM_IN, key) ? 2 : .5), .25, 4);
      MapConfig.queued(s);
    }
  }

  public static Waypoint newWaypoint() {
    Waypoint p = new Waypoint();
    p.x = (int) Math.floor(Maps.x());
    p.y = Math.max(0, Math.min(127, (int) Math.floor(Maps.y())));
    p.z = (int) Math.floor(Maps.z());
    p.dimension = Maps.dimension();
    if (Maps.waypoints() != null) p.set = Maps.waypoints().selectedSet();
    return p;
  }
}
