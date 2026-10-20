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
      RADAR_ICONS = new KeyBinding("powerbeta.radarIcons", Keyboard.KEY_TAB),
      ZOOM_IN = new KeyBinding("powerbeta.mapZoomIn", Keyboard.KEY_I),
      ZOOM_OUT = new KeyBinding("powerbeta.mapZoomOut", Keyboard.KEY_O),
      TOGGLE = new KeyBinding("powerbeta.minimap", 0),
      WORLD_WAYPOINTS = new KeyBinding("powerbeta.toggleWorldWaypoints", 0),
      MAP_WAYPOINTS = new KeyBinding("powerbeta.toggleMapWaypoints", 0),
      RADAR = new KeyBinding("powerbeta.toggleMapRadar", 0),
      CAVES = new KeyBinding("powerbeta.toggleMapCaves", 0),
      GRID = new KeyBinding("powerbeta.toggleMapGrid", 0),
      CLEAR_DESTINATION = new KeyBinding("powerbeta.clearMapDestination", 0),
      SETTINGS = new KeyBinding("powerbeta.mapSettings", 0),
      ZOOM_RESET = new KeyBinding("powerbeta.mapZoomReset", 0);

  private static final List<KeyBinding> ACTIONS = List.of(TOGGLE, ZOOM_IN, ZOOM_OUT,
      WORLD_WAYPOINTS, MAP_WAYPOINTS, RADAR, CAVES, GRID, CLEAR_DESTINATION, SETTINGS, ZOOM_RESET);
  private static final List<KeyBinding> ALL = List.of(MAP, WAYPOINT, WAYPOINTS, ENLARGE, RADAR_ICONS,
      ZOOM_IN, ZOOM_OUT, TOGGLE, WORLD_WAYPOINTS, MAP_WAYPOINTS, RADAR, CAVES, GRID,
      CLEAR_DESTINATION, SETTINGS, ZOOM_RESET);

  public static List<KeyBinding> all() {
    return ALL;
  }

  public static boolean available(String id) {
    return available(id, MapConfig.current());
  }

  static boolean available(String id, MapSettings settings) {
    return switch (id) {
      case "powerbeta.map" -> settings.worldMap;
      case "powerbeta.enlargeMap", "powerbeta.mapZoomIn", "powerbeta.mapZoomOut", "powerbeta.mapZoomReset" ->
          settings.minimap;
      case "powerbeta.radarIcons" -> settings.radar && (settings.minimap || settings.worldMap);
      case "powerbeta.toggleMapWaypoints", "powerbeta.toggleMapRadar", "powerbeta.toggleMapCaves",
          "powerbeta.toggleMapGrid", "powerbeta.clearMapDestination" -> settings.minimap || settings.worldMap;
      default -> true;
    };
  }

  public static boolean iconsHeld(Minecraft mc) {
    if (mc.world == null || mc.player == null || mc.player.dead || mc.player.health <= 0
        || !Display.isActive()) return false;
    if (mc.currentScreen instanceof local.luke.power.ui.WorldMapScreen map) {
      if (!map.radarIconsAllowed()) return false;
    } else if (mc.currentScreen instanceof local.luke.power.ui.HudEditorScreen hud) {
      if (!hud.radarIconsAllowed()) return false;
    } else if (mc.currentScreen != null || mc.paused) return false;
    return Bindings.down(RADAR_ICONS);
  }

  /** Mutates only the caller's snapshot; the input dispatcher queues it once afterward. */
  static boolean change(String id, MapSettings settings) {
    if (!available(id, settings)) return false;
    switch (id) {
      case "powerbeta.minimap" -> settings.minimap = !settings.minimap;
      case "powerbeta.toggleWorldWaypoints" -> settings.worldWaypoints = !settings.worldWaypoints;
      case "powerbeta.toggleMapWaypoints" -> settings.waypoints = !settings.waypoints;
      case "powerbeta.toggleMapRadar" -> settings.radar = !settings.radar;
      case "powerbeta.toggleMapCaves" -> settings.caveMode = (settings.caveMode + 1) % 3;
      case "powerbeta.toggleMapGrid" -> settings.chunkGrid = !settings.chunkGrid;
      case "powerbeta.mapZoomIn" -> settings.zoom = MapMath.bounded(settings.zoom * 2, .25, 4);
      case "powerbeta.mapZoomOut" -> settings.zoom = MapMath.bounded(settings.zoom * .5, .25, 4);
      case "powerbeta.mapZoomReset" -> settings.zoom = 1;
      default -> { return false; }
    }
    return true;
  }

  public static void press(Minecraft mc, int key) {
    if (mc.world == null
        || mc.player == null
        || mc.currentScreen != null
        || mc.paused
        || mc.player.dead
        || mc.player.health <= 0
        || !Display.isActive()) return;
    if (available(MAP.translationKey) && Bindings.matches(MAP, key))
      mc.setScreen(new local.luke.power.ui.WorldMapScreen());
    else if (Bindings.matches(WAYPOINT, key)) {
      Maps.ensureWaypoints(mc);
      mc.setScreen(new local.luke.power.ui.WaypointEditorScreen(null, newWaypoint()));
    } else if (Bindings.matches(WAYPOINTS, key)) {
      Maps.ensureWaypoints(mc);
      mc.setScreen(new local.luke.power.ui.WaypointsScreen(null));
    }
    else for (KeyBinding action : ACTIONS) {
      if (!available(action.translationKey) || !Bindings.matches(action, key)) continue;
      if (action == CLEAR_DESTINATION) Maps.destination(null);
      else if (action == SETTINGS) local.luke.power.ui.PowerOptionsScreen.openMap(mc, null);
      else {
        var snapshot = MapConfig.copy();
        if (change(action.translationKey, snapshot)) MapConfig.queued(snapshot);
      }
      return;
    }
  }

  public static Waypoint newWaypoint() {
    Minecraft mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    var pose = mc.player == null ? null : local.luke.power.input.ReportedPosition.current(mc.player,
        mc.player.x, mc.player.boundingBox.minY, mc.player.z, mc.player.yaw, mc.player.pitch);
    Waypoint p = new Waypoint();
    p.x = (int) Math.floor(MapMath.bounded(pose == null ? Maps.x() : pose.x(), -MapMath.BORDER, MapMath.BORDER));
    p.y = Math.max(0, Math.min(127, (int) Math.floor(pose == null ? Maps.y() : pose.y())));
    p.z = (int) Math.floor(MapMath.bounded(pose == null ? Maps.z() : pose.z(), -MapMath.BORDER, MapMath.BORDER));
    p.dimension = mc.world == null ? Maps.dimension() : mc.world.dimension.id;
    if (Maps.waypoints() != null) p.set = Maps.waypoints().selectedSet();
    return p;
  }
}
