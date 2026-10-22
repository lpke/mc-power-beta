package local.luke.power.map;

import java.util.*;
import local.luke.power.render.EntityVisibility;
import local.luke.power.visual.VisualConfig;
import local.luke.power.visual.VisualSettings;
import net.minecraft.class_43;
import net.minecraft.class_65;
import net.minecraft.class_258;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Loaded-terrain discovery and tick-owned snapshots, shared by the two map views. */
final class RadarTracking {
  private static final int CHUNKS_PER_TICK = 64, ENTITIES_PER_TICK = 512;
  private final RadarChunkScan<class_43, Entity> scan = new RadarChunkScan<>();
  private final RadarChunkScan<class_43, Entity> filteredScan = new RadarChunkScan<>();
  private final RadarSelection<Entity> minimap = new RadarSelection<>(), worldMap = new RadarSelection<>();
  private List<Maps.Radar> minimapSnapshot = List.of(), worldMapSnapshot = List.of();
  private boolean compactIndex;

  List<Maps.Radar> snapshot(boolean mini) { return mini ? minimapSnapshot : worldMapSnapshot; }

  static int range(int requested, int renderChunks) {
    return Math.min(Math.max(1, Math.min(32, requested)), Math.max(1, renderChunks));
  }

  static boolean minimapTypesVisible(MapSettings settings, VisualSettings visibility) {
    return !visibility.hideEntities || settings.players && !visibility.hidePlayers
        || (settings.hostile || settings.animals) && !visibility.hideMobs
        || settings.items && !visibility.hideItems
        || settings.otherEntities && (!visibility.hideMobs || !visibility.hideVehicles
            || !visibility.hideProjectiles || !visibility.hideTnt || !visibility.hideOther);
  }

  static boolean compactIndex(MapSettings settings, boolean full, VisualSettings visibility) {
    boolean ignoreHidden = full || !visibility.hideEntities;
    return !(settings.items && (ignoreHidden || !visibility.hideItems))
        && !(settings.otherEntities && (ignoreHidden || !visibility.hideTnt));
  }

  static double previous(double previous, double current, boolean paused, boolean remote) {
    return paused && !remote ? current : previous;
  }

  static boolean filteredLane(MapSettings settings, boolean mini, boolean full, VisualSettings visibility) {
    return mini && full && compactIndex(settings, false, visibility) && !compactIndex(settings, true, visibility);
  }

  void tick(Minecraft mc, MapSettings settings, boolean mini, boolean full, int tick,
      double x, double y, double z) {
    boolean types = settings.players || settings.hostile || settings.animals || settings.items || settings.otherEntities;
    full &= types;
    boolean activeMini = mini && types && minimapTypesVisible(settings, VisualConfig.current());
    if (!activeMini && !full) { clear(); return; }
    int radius = range(settings.radarRange, local.luke.power.controls.util.ModOptions.getRenderDistanceChunks());
    double radiusSquared = radius * radius * 256d;
    // The shared collision index is maintained even with its fix disabled and retains custom
    // subclasses. It is safe only when both exact native classes it omits are unwanted.
    var visibility = VisualConfig.current();
    boolean nextCompact = compactIndex(settings, full, visibility);
    if (compactIndex != nextCompact) { scan.clear(); compactIndex = nextCompact; }

    minimap.refresh(activeMini ? settings.radarLimit : 0,
        entity -> eligible(mc, settings, entity, false, x, y, z, radiusSquared));
    worldMap.refresh(full ? settings.radarLimit : 0,
        entity -> eligible(mc, settings, entity, true, x, y, z, radiusSquared));
    final boolean fullView = full;
    java.util.function.Consumer<Entity> visit = entity -> {
          if (!fullView && EntityVisibility.hidden(entity)) return;
          String nativeType = net.minecraft.class_206.method_734(entity);
          if (!kind(entity, nativeType).visible(settings)) return;
          double distance = position(mc, entity, x, y, z, settings.radarHeight, radiusSquared);
          if (!(activeMini && minimap.accepts(distance)) && !(fullView && worldMap.accepts(distance))) return;
          if (activeMini && !EntityVisibility.hidden(entity)) minimap.offer(entity, distance);
          if (fullView) worldMap.offer(entity, distance);
        };
    // The open World Map may need native items/TNT while its minimap preview hides them.
    // Reserve half the existing budget for that preview's compact index so opening the
    // sidebar during a storm cannot bury its first visible mob behind thousands of items.
    boolean separate = filteredLane(settings, activeMini, full, visibility);
    int divisor = separate ? 2 : 1;
    scan.scan(access(mc, compactIndex), (int)Math.floor(x / 16), (int)Math.floor(z / 16), radius,
        y - settings.radarHeight, y + settings.radarHeight,
        CHUNKS_PER_TICK / divisor, ENTITIES_PER_TICK / divisor, visit);
    if (separate)
      filteredScan.scan(access(mc, true), (int)Math.floor(x / 16), (int)Math.floor(z / 16), radius,
          y - settings.radarHeight, y + settings.radarHeight,
          CHUNKS_PER_TICK / 2, ENTITIES_PER_TICK / 2, visit);
    else filteredScan.clear();
    var captured = new IdentityHashMap<Entity, Maps.Radar>();
    boolean icons = MapKeys.iconsShown(mc, true, settings) || MapKeys.iconsShown(mc, false, settings);
    minimapSnapshot = capture(minimap.values(), settings, tick, mc.paused, mc.world.isRemote, captured, icons);
    worldMapSnapshot = capture(worldMap.values(), settings, tick, mc.paused, mc.world.isRemote, captured, icons);
  }

  private static RadarChunkScan.Access<class_43, Entity> access(Minecraft mc, boolean compact) {
    return new RadarChunkScan.Access<>() {
      public class_43 loaded(int cx, int cz) { return MapSampler.loaded(mc.world, cx, cz); }
      @SuppressWarnings("unchecked") public List<Entity>[] sections(class_43 chunk) {
        return compact && chunk instanceof RadarChunkAccess indexed
            ? indexed.power$radarEntities() : chunk.field_965;
      }
      public int bottom(class_43 chunk) {
        return chunk instanceof RadarChunkAccess indexed ? indexed.power$radarBottom() : 0;
      }
    };
  }

  private static double eligible(Minecraft mc, MapSettings settings, Entity entity, boolean full,
      double x, double y, double z, double radiusSquared) {
    if (!kind(entity, net.minecraft.class_206.method_734(entity)).visible(settings)) return Double.NaN;
    if (!full && EntityVisibility.hidden(entity)) return Double.NaN;
    double distance = position(mc, entity, x, y, z, settings.radarHeight, radiusSquared);
    if (!Double.isFinite(distance) || MapSampler.loaded(mc.world,
        (int)Math.floor(entity.x / 16), (int)Math.floor(entity.z / 16)) == null) return Double.NaN;
    return distance;
  }

  private static double position(Minecraft mc, Entity entity, double x, double y, double z,
      int height, double radiusSquared) {
    if (entity == mc.player || entity.dead || entity.world != mc.world
        || !Double.isFinite(entity.y) || Math.abs(entity.y - y) > height) return Double.NaN;
    double dx = entity.x - x, dz = entity.z - z, distance = dx * dx + dz * dz;
    return distance <= radiusSquared ? distance : Double.NaN;
  }

  private static List<Maps.Radar> capture(Collection<Entity> selected, MapSettings settings, int tick, boolean paused, boolean remote,
      IdentityHashMap<Entity, Maps.Radar> captured, boolean icons) {
    var result = new ArrayList<Maps.Radar>(selected.size());
    for (Entity entity : selected) {
      var snapshot = captured.get(entity);
      if (snapshot == null) {
        String nativeType = net.minecraft.class_206.method_734(entity);
        RadarKind kind = kind(entity, nativeType);
        String name = entity instanceof PlayerEntity player ? player.name : nativeType;
        if (name == null || name.isBlank()) name = "Entity";
        snapshot = new Maps.Radar(entity.x, entity.y, entity.z, name,
            settings.mobColor(nativeType, kind.color(settings)), entity instanceof net.minecraft.class_142,
            previous(entity.prevX, entity.x, paused, remote), previous(entity.prevY, entity.y, paused, remote),
            previous(entity.prevZ, entity.z, paused, remote), tick, kind, nativeType,
            icons ? RadarIconSource.capture(entity, nativeType) : null);
        captured.put(entity, snapshot);
      }
      result.add(snapshot);
    }
    return List.copyOf(result);
  }

  private static RadarKind kind(Entity entity, String nativeType) {
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

  void clear() {
    scan.clear(); filteredScan.clear(); minimap.clear(); worldMap.clear();
    minimapSnapshot = worldMapSnapshot = List.of();
  }
}
