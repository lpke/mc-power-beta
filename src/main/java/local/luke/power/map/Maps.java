package local.luke.power.map;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.commands.CommandContext;
import local.luke.power.input.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_258;
import net.minecraft.class_65;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/** One session supplies both views. It owns no simulation or player inventory state. */
public final class Maps {
  public record Radar(
      double x,
      double y,
      double z,
      String name,
      int color,
      boolean item,
      double previousX,
      double previousY,
      double previousZ,
      int tick,
      RadarKind kind,
      String nativeType,
      RadarIconSource icon) {
    public Radar(double x, double y, double z, String name, int color, boolean item,
        double previousX, double previousY, double previousZ, int tick) {
      this(x, y, z, name, color, item, previousX, previousY, previousZ, tick, null, null, null);
    }
    public int color(MapSettings settings) {
      return kind == null ? color : settings.mobColor(nativeType, kind.color(settings));
    }
  }

  public record Step(double x, double z, int dimension) {}

  private static MapPalette palette;
  private static final DeathpointQueue DEATHS = new DeathpointQueue();
  private static final ArrayDeque<Step> PATH = new ArrayDeque<>();
  private static List<Radar> radar = List.of();
  private static final List<Radar> radarScan = new ArrayList<>();
  private static long pointsRevision = Long.MIN_VALUE;
  private static int radarSize, radarStyle;
  private static List<Waypoint> points = List.of();
  private static Waypoint destination;
  private static boolean destinationArrivalArmed;
  private static MapSettings radarPreview;

  /** The HUD supplies an isolated snapshot; entity reads still happen only on ticks. */
  public static void previewRadar(MapSettings settings) { radarPreview = settings; }
  private static MapCache cache;
  private static WaypointStore waypoints;
  private static MapSampler sampler;
  private static World world;
  private static String identity = "", server = "", submap = "Default", error = "";
  private static double x, y, z;
  private static float yaw;
  private static int dimension, layer, ticks, entityCursor;
  private static boolean wasDead, explicitWaypoints, minimapActive;
  private static long lastError;

  private Maps() {}

  public static MapCache cache() {
    return cache;
  }

  public static WaypointStore waypoints() {
    return waypoints;
  }

  public static List<Waypoint> points() {
    return points;
  }

  public static void destination(Waypoint point) {
    destinationArrivalArmed = false;
    destination = point == null ? null : point.copy();
    if (destination != null) {
      destination.id = "temporary-destination";
      destination.name = "Destination";
      destination.global = true;
      destination.enabled = true;
    }
    pointsRevision = Long.MIN_VALUE;
  }

  private static void updateDestination() {
    if (destination != null && destination.dimension == dimension) {
      double distance = destination.distance(x, z);
      if (distance >= 2) destinationArrivalArmed = true;
      else if (destinationArrivalArmed) destination(null);
    }
  }

  public static List<Radar> radar() {
    return radar;
  }

  public static float radarDelta(Radar entity, float delta) {
    return ticks - entity.tick() <= 1 ? delta : 1;
  }

  public static Collection<Step> path() {
    return Collections.unmodifiableCollection(PATH);
  }

  public static double x() {
    return x;
  }

  public static double y() {
    return y;
  }

  public static double z() {
    return z;
  }

  public static float yaw() {
    return yaw;
  }

  public static ReportedPosition.Pose framePose(Minecraft mc, float delta) {
    if (mc.player == null || mc.world != world) return new ReportedPosition.Pose(x, y, z, yaw, 0);
    if (DetachedCamera.isActive())
      return ReportedPosition.current(mc.player, x, y, z, yaw, mc.player.pitch);
    var p = mc.player;
    return new ReportedPosition.Pose(
        MapMotion.position(p.prevX, p.x, delta),
        local.luke.power.hud.HudCoordinates.y(p) + MapMotion.position(p.prevY, p.y, delta) - p.y,
        MapMotion.position(p.prevZ, p.z, delta),
        CameraView.yaw(MapMotion.angle(p.prevYaw, p.yaw, delta)),
        CameraView.pitch(MapMotion.angle(p.prevPitch, p.pitch, delta)));
  }

  public static int dimension() {
    return dimension;
  }

  public static int layer() {
    return layer;
  }

  public static String error() {
    return error;
  }

  public static String submap() {
    return submap;
  }

  public static MapSampler sampler() {
    return sampler;
  }

  public static void server(String address, int port) {
    server = address.trim().toLowerCase(Locale.ROOT) + ":" + port;
    submap = "Default";
  }

  public static void submap(String name) {
    Waypoint.text(name, 48);
    submap = name;
  }

  public static Path directory(String identity) {
    return FabricLoader.getInstance()
        .getGameDir()
        .resolve("power-beta-data/maps")
        .resolve(UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString());
  }

  public static void error(String message) {
    error = message;
    long now = System.currentTimeMillis();
    if (now - lastError >= 10000) {
      local.luke.power.PowerBeta.LOG.error(message);
      lastError = now;
    }
  }

  /** Explicit waypoint actions can open their store even while all display features are off. */
  public static WaypointStore ensureWaypoints(Minecraft mc) {
    if (mc.world == null || mc.player == null) return null;
    explicitWaypoints = true;
    // World transitions are prepared on ticks, where leaving may finish durable work.
    if (world != mc.world || identity.isEmpty()) return null;
    if (waypoints == null) waypoints = new WaypointStore(directory(identity), Maps::error);
    waypoints.tick();
    return waypoints;
  }

  private static boolean waypointScreen(net.minecraft.client.gui.screen.Screen screen) {
    for (int depth = 0; screen != null && depth < 8; depth++) {
      if (screen instanceof local.luke.power.ui.WaypointsScreen
          || screen instanceof local.luke.power.ui.WaypointEditorScreen) return true;
      screen = screen instanceof local.luke.power.ui.ColourScreen color ? color.parentScreen() : null;
    }
    return false;
  }

  private static void prepareWorld(Minecraft mc) {
    String next = mc.world.isRemote ? "server:" + server + "/" + submap : "world:" + CommandContext.world(mc);
    if (!next.equals(identity)) {
      leave();
      identity = next;
      error = "";
      wasDead = false;
    }
    if (world != mc.world) {
      world = mc.world;
      PATH.clear();
      clearRadar();
    }
  }

  private static void clearRadar() {
    radar = List.of(); radarScan.clear(); entityCursor = 0;
  }

  public static void tick(Minecraft mc) {
    if (mc.world == null || mc.player == null) {
      if (!identity.isEmpty() || cache != null || waypoints != null) leave();
      world = null;
      return;
    }
    prepareWorld(mc);
    MapSettings settings = MapConfig.current();
    explicitWaypoints = waypointScreen(mc.currentScreen);
    MapActivity activity = MapActivity.of(settings, explicitWaypoints);
    if (!(mc.currentScreen instanceof local.luke.power.ui.HudEditorScreen)) radarPreview = null;
    MapSettings radarSettings = radarPreview == null ? settings : radarPreview;
    boolean radarEnabled = activity.terrain() && MapActivity.of(radarSettings, false).radar();
    if (minimapActive && !settings.minimap) MinimapBuffer.clear();
    minimapActive = settings.minimap;
    if (!activity.terrain()) retireTerrain();
    if (!radarEnabled) clearRadar();
    if (!activity.path()) PATH.clear();
    var pose = ReportedPosition.current(mc.player, mc.player.x, mc.player.boundingBox.minY,
        mc.player.z, mc.player.yaw, mc.player.pitch);
    if (!pose.finite()) return;
    x = MapMath.bounded(pose.x(), -MapMath.BORDER, MapMath.BORDER);
    y = pose.y(); z = MapMath.bounded(pose.z(), -MapMath.BORDER, MapMath.BORDER);
    yaw = pose.yaw(); dimension = mc.world.dimension.id;
    updateDestination();

    if (activity.terrain()) {
      if (palette == null) palette = new MapPalette();
      if (cache == null) cache = new MapCache(directory(identity), settings.cacheTiles, Maps::error);
      cache.resume();
      if (sampler == null) sampler = new MapSampler(palette);
      palette.tick(mc);
      cache.limit(settings.cacheTiles);
      // The live cave context also serves a map screen switched from Surface to Caves.
      cache.viewContext(MapSampler.window(settings, y).context());
      cache.tick();
      layer = MapSampler.layer(mc, x, y, z);
      sampler.tick(mc, cache, layer, x, y, z);
    }

    MapLightShading.tick(mc, radarSettings);

    boolean dead = mc.player.dead || mc.player.health <= 0;
    if (activity.waypoints() && waypoints == null)
      waypoints = new WaypointStore(directory(identity), Maps::error);
    if (waypoints != null) {
      // Existing edits still finish if their display is disabled during an asynchronous save.
      if (activity.waypoints() || waypoints.needsMaintenance()) waypoints.tick(activity.waypoints());
      if (dead && !wasDead && settings.deathpoints) {
        try {
          DEATHS.capture((int)Math.floor(mc.player.x), (int)Math.floor(mc.player.boundingBox.minY),
              (int)Math.floor(mc.player.z), dimension, settings.keepOldDeaths);
        } catch (RuntimeException e) { error("Death waypoint could not be recorded: " + e.getMessage()); }
      }
      try { DEATHS.flush(waypoints); }
      catch (RuntimeException e) { error("Death waypoint could not be recorded: " + e.getMessage()); }
      if (activity.waypoints() && (pointsRevision != waypoints.revision() || pointsRevision == Long.MIN_VALUE)) {
        var snapshot = new ArrayList<>(waypoints.points());
        if (destination != null) snapshot.add(0, destination);
        points = List.copyOf(snapshot);
        pointsRevision = waypoints.ready() ? waypoints.revision() : Long.MIN_VALUE;
      }
    }
    wasDead = dead;
    if (radarEnabled) radar(mc, radarSettings);
    else clearRadar();
    if (activity.path() && ticks % 10 == 0 && !dead) {
      Step last = PATH.peekLast();
      if (last == null || last.dimension() != dimension || Math.hypot(last.x() - x, last.z() - z) >= 1) {
        PATH.addLast(new Step(x, z, dimension));
        while (PATH.size() > 512) PATH.removeFirst();
      }
    } else if (!activity.path()) PATH.clear();
    ticks++;
  }

  private static void retireTerrain() {
    // Terrain callbacks become inert immediately; only existing durable work may continue.
    sampler = null;
    palette = null;
    layer = -1;
    if (cache == null) return;
    cache.retire();
    cache.tick();
    if (cache.releaseIfIdle()) {
      cache = null;
      MapRenderer.clear();
      MinimapBuffer.clear();
    }
  }

  private static void radar(Minecraft mc, MapSettings settings) {
    List<Radar> result = new ArrayList<>();
    List<?> entities = mc.world.field_198;
    int size = entities.size();
    if (size == 0) {
      radar = List.of();
      radarScan.clear();
      entityCursor = 0;
      return;
    }
    int style =
        Objects.hash(
            settings.players,
            settings.hostile,
            settings.animals,
            settings.items,
            settings.otherEntities,
            settings.radarHeight,
            settings.radarLimit,
            settings.radarColorStyle());
    if (size != radarSize || style != radarStyle || entityCursor >= size) {
      entityCursor = 0;
      radarScan.clear();
      radarSize = size;
      radarStyle = style;
    }
    int end = Math.min(size, entityCursor + settings.radarLimit);
    for (int n = entityCursor; n < end; n++) {
      Entity entity = (Entity) entities.get(n);
      if (entity == mc.player
          || entity.dead
          || Math.abs(entity.y - y) > settings.radarHeight
          || Math.hypot(entity.x - x, entity.z - z) > 256) continue;
      String nativeType = net.minecraft.class_206.method_734(entity);
      RadarKind kind = radarKind(entity, nativeType);
      if (!kind.visible(settings)) continue;
      int color = settings.mobColor(nativeType, kind.color(settings));
      boolean item = entity instanceof net.minecraft.class_142;
      String name =
          entity instanceof PlayerEntity p ? p.name : net.minecraft.class_206.method_734(entity);
      if (name == null || name.isBlank()) name = "Entity";
      result.add(
          new Radar(
              entity.x,
              entity.y,
              entity.z,
              name,
              color,
              item,
              entity.prevX,
              entity.prevY,
              entity.prevZ,
              ticks,
              kind,
              nativeType,
              RadarIconSource.capture(entity, nativeType)));
    }
    radarScan.addAll(result);
    if (radarScan.size() > settings.radarLimit) {
      radarScan.sort(Comparator.comparingDouble(p -> Math.hypot(p.x() - x, p.z() - z)));
      radarScan.subList(settings.radarLimit, radarScan.size()).clear();
    }
    entityCursor = end;
    if (entityCursor == size) {
      radar = List.copyOf(radarScan);
      radarScan.clear();
      entityCursor = 0;
    }
  }

  private static RadarKind radarKind(Entity entity, String nativeType) {
    if (entity instanceof PlayerEntity) return RadarKind.PLAYERS;
    if (nativeType != null) {
      switch (nativeType) {
        case "Creeper", "Skeleton", "Spider", "Giant", "Zombie", "Slime", "Ghast", "PigZombie", "Monster" -> { return RadarKind.HOSTILE; }
        case "Pig", "Sheep", "Cow", "Chicken", "Squid", "Wolf" -> { return RadarKind.ANIMALS; }
        case "Item" -> { return RadarKind.ITEMS; }
        case "Boat", "Minecart" -> { return RadarKind.VEHICLES; }
        case "Arrow", "Snowball" -> { return RadarKind.PROJECTILES; }
      }
    }
    if (entity instanceof class_65) return RadarKind.HOSTILE;
    if (entity instanceof class_258 || entity instanceof WaterCreatureEntity) return RadarKind.ANIMALS;
    if (entity instanceof net.minecraft.class_142) return RadarKind.ITEMS;
    if (entity instanceof net.minecraft.class_372 || entity instanceof net.minecraft.class_499
        || entity instanceof net.minecraft.entity.projectile.FishingBobberEntity) return RadarKind.PROJECTILES;
    return RadarKind.OTHER;
  }

  public static void leave() {
    MapDeathCauses.clear();
    MapLightShading.clear();
    if (cache != null) cache.finish();
    if (waypoints != null) {
      waypoints.finish();
      DEATHS.retry();
      try {
        while (DEATHS.flush(waypoints)) {}
      } catch (RuntimeException e) {
        error("Death waypoint could not be saved: " + e.getMessage());
      }
      waypoints.finish();
      if (DEATHS.size() > 0)
        error(DEATHS.size() + " death waypoints could not be saved; existing data was retained");
    }
    if (cache != null || waypoints != null) MapIO.awaitQueued();
    MapRenderer.clear();
    MinimapBuffer.clear();
    cache = null;
    waypoints = null;
    sampler = null;
    explicitWaypoints = false;
    minimapActive = false;
    identity = "";
    PATH.clear();
    radar = List.of();
    radarScan.clear();
    points = List.of();
    pointsRevision = Long.MIN_VALUE;
    destination = null;
    destinationArrivalArmed = false;
    radarPreview = null;
    DEATHS.clear();
  }

  public static boolean canTeleport(Minecraft mc, Waypoint point) {
    return mc.world != null
        && !mc.world.isRemote
        && mc.player != null
        && point.dimension == mc.world.dimension.id
        && !DetachedCamera.isActive()
        && !mc.player.dead
        && mc.player.health > 0
        && local.luke.power.permissions.CommandPermissions.allowed(
            local.luke.power.rules.GameRule.ALLOW_TELEPORTATION);
  }

  public static void teleport(Minecraft mc, Waypoint point) {
    if (!canTeleport(mc, point))
      throw new IllegalArgumentException(
          "Teleportation requires local teleport access, the same dimension and the player's view");
    // Use the existing command implementation so history, ownership and position reporting agree.
    mc.player.sendChatMessage("/tp " + (point.x + .5) + " " + point.y + " " + (point.z + .5));
  }
}
