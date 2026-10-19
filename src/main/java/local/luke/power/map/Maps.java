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
  public record Radar(double x, double y, double z, String name, int colour, boolean item) {}

  public record Step(double x, double z, int dimension) {}

  private static final MapPalette PALETTE = new MapPalette();
  private static final DeathpointQueue DEATHS = new DeathpointQueue();
  private static final ArrayDeque<Step> PATH = new ArrayDeque<>();
  private static List<Radar> radar = List.of();
  private static final List<Radar> radarScan = new ArrayList<>();
  private static long pointsRevision = Long.MIN_VALUE;
  private static int radarSize, radarStyle;
  private static List<Waypoint> points = List.of();
  private static Waypoint destination;
  private static MapCache cache;
  private static WaypointStore waypoints;
  private static MapSampler sampler;
  private static World world;
  private static String identity = "", server = "", submap = "Default", error = "";
  private static double x, y, z;
  private static float yaw;
  private static int dimension, layer, ticks, entityCursor;
  private static boolean wasDead;
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
    destination = point == null ? null : point.copy();
    if (destination != null) {
      destination.id = "temporary-destination";
      destination.name = "Destination";
      destination.global = true;
      destination.enabled = true;
    }
    pointsRevision = Long.MIN_VALUE;
  }

  public static List<Radar> radar() {
    return radar;
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

  public static void tick(Minecraft mc) {
    if (mc.world == null || mc.player == null) {
      if (cache != null) leave();
      world = null;
      return;
    }
    String next =
        mc.world.isRemote ? "server:" + server + "/" + submap : "world:" + CommandContext.world(mc);
    if (!next.equals(identity) || cache == null) {
      leave();
      identity = next;
      error = "";
      Path root = directory(next);
      cache = new MapCache(root, MapConfig.current().cacheTiles, Maps::error);
      waypoints = new WaypointStore(root, Maps::error);
      sampler = new MapSampler(PALETTE);
      wasDead = false;
      points = List.of();
      pointsRevision = Long.MIN_VALUE;
    }
    if (world != mc.world) {
      world = mc.world;
      PATH.clear();
      radar = List.of();
      radarScan.clear();
      entityCursor = 0;
    }
    var pose =
        ReportedPosition.current(
            mc.player,
            mc.player.x,
            mc.player.boundingBox.minY,
            mc.player.z,
            mc.player.yaw,
            mc.player.pitch);
    if (!pose.finite()) return;
    x = MapMath.bounded(pose.x(), -MapMath.BORDER, MapMath.BORDER);
    y = pose.y();
    z = MapMath.bounded(pose.z(), -MapMath.BORDER, MapMath.BORDER);
    yaw = pose.yaw();
    dimension = mc.world.dimension.id;
    MapSettings settings = MapConfig.current();
    if (destination != null && destination.dimension == dimension && destination.distance(x, z) < 2)
      destination(null);
    PALETTE.tick(mc);
    cache.limit(settings.cacheTiles);
    cache.tick();
    waypoints.tick();
    boolean dead = mc.player.dead || mc.player.health <= 0;
    if (dead && !wasDead && settings.deathpoints) {
      try {
        DEATHS.capture(
            (int) Math.floor(mc.player.x),
            (int) Math.floor(mc.player.boundingBox.minY),
            (int) Math.floor(mc.player.z),
            dimension,
            settings.keepOldDeaths);
      } catch (RuntimeException e) {
        error("Death waypoint could not be recorded: " + e.getMessage());
      }
    }
    wasDead = dead;
    try {
      DEATHS.flush(waypoints);
    } catch (RuntimeException e) {
      error("Death waypoint could not be recorded: " + e.getMessage());
    }
    if (pointsRevision != waypoints.revision() || pointsRevision == Long.MIN_VALUE) {
      var snapshot = new ArrayList<>(waypoints.points());
      if (destination != null) snapshot.add(0, destination);
      points = List.copyOf(snapshot);
      pointsRevision = waypoints.ready() ? waypoints.revision() : Long.MIN_VALUE;
    }
    layer = MapSampler.layer(mc, x, y, z);
    if (settings.minimap || settings.worldMap) sampler.tick(mc, cache, layer, x, y, z);
    if (settings.radar) radar(mc, settings);
    else {
      radar = List.of();
      radarScan.clear();
      entityCursor = 0;
    }
    if (settings.footsteps && ticks % 10 == 0 && !dead) {
      Step last = PATH.peekLast();
      if (last == null
          || last.dimension() != dimension
          || Math.hypot(last.x() - x, last.z() - z) >= 1) {
        PATH.addLast(new Step(x, z, dimension));
        while (PATH.size() > 512) PATH.removeFirst();
      }
    }
    ticks++;
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
            settings.radarLimit);
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
      int colour;
      boolean item = entity instanceof net.minecraft.class_142;
      if (entity instanceof PlayerEntity) {
        if (!settings.players) continue;
        colour = 0xffffff;
      } else if (entity instanceof class_65) {
        if (!settings.hostile) continue;
        colour = 0xff6655;
      } else if (entity instanceof class_258 || entity instanceof WaterCreatureEntity) {
        if (!settings.animals) continue;
        colour = 0xffff55;
      } else if (item) {
        if (!settings.items) continue;
        colour = 0xff5555;
      } else {
        if (!settings.otherEntities) continue;
        colour = 0xcc66ff;
      }
      String name =
          entity instanceof PlayerEntity p ? p.name : net.minecraft.class_206.method_734(entity);
      if (name == null || name.isBlank()) name = "Entity";
      result.add(new Radar(entity.x, entity.y, entity.z, name, colour, item));
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

  public static void leave() {
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
    cache = null;
    waypoints = null;
    sampler = null;
    identity = "";
    PATH.clear();
    radar = List.of();
    radarScan.clear();
    points = List.of();
    pointsRevision = Long.MIN_VALUE;
    destination = null;
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
