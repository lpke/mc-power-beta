package local.luke.power.map;

import java.util.List;
import java.util.Map;

/** Shared quick settings. Ordinals are the retained middle-click action IDs. */
public enum MapToggle implements MapTogglePanel.Toggle {
  ITEMS("Show items", "items"),
  MOBS("Show mobs", "hostile", "animals"),
  WAYPOINTS("Show waypoints", "waypoints"),
  WORLD_WAYPOINTS("Show in-game waypoints", "worldWaypoints"),
  CAVES("Cave mode", "caveMode"),
  HEADS("Mob heads", "worldMapRadarIcons"),
  CHUNKS("Chunk borders", "chunkGrid"),
  SLIME("Slime chunks", "slimeChunks"),
  LIGHT("Spawnable overlay", "lightOverlay"),
  PATH("Path", "footsteps"),
  RENDERED_AREA("Show rendered area", "renderedArea");

  public static final List<MapToggle> ALL = List.of(values());
  public static final List<MapToggle> WORLD_MAP = List.of(ITEMS, MOBS, WAYPOINTS, WORLD_WAYPOINTS,
      HEADS, CHUNKS, RENDERED_AREA, SLIME, LIGHT, PATH);
  public final String label;
  public final List<String> fields;
  private static int rememberedCaveMode = 1;
  MapToggle(String label, String... fields) { this.label = label; this.fields = List.of(fields); }
  @Override public String label() { return label; }

  public boolean enabled(MapSettings settings, boolean minimap) {
    return switch (this) {
      case ITEMS -> settings.items;
      case MOBS -> settings.hostile && settings.animals;
      case WAYPOINTS -> settings.waypoints;
      case WORLD_WAYPOINTS -> settings.worldWaypoints;
      case CAVES -> settings.caveMode != 0;
      case HEADS -> minimap ? MapKeys.minimapHeadsToggled() : settings.worldMapRadarIcons;
      case CHUNKS -> settings.chunkGrid;
      case SLIME -> settings.slimeChunks;
      case LIGHT -> settings.lightOverlay;
      case PATH -> settings.footsteps;
      case RENDERED_AREA -> settings.renderedArea;
    };
  }

  /** Each multi-field toggle is applied as one Options edit or queued snapshot. */
  public Map<String, Object> changes(MapSettings settings) {
    boolean next = !enabled(settings, false);
    if (this == CAVES) {
      if (settings.caveMode != 0) rememberedCaveMode = settings.caveMode;
      return Map.of("caveMode", next ? rememberedCaveMode : 0);
    }
    if (this == MOBS) return Map.of("hostile", next, "animals", next);
    return Map.of(fields.get(0), next);
  }

  public void toggleMinimap() {
    if (this == HEADS) { MapKeys.toggleMinimapHeads(); return; }
    var copy = MapConfig.copy();
    apply(copy, changes(copy));
    MapConfig.queued(copy);
  }

  public static void apply(MapSettings settings, Map<String, Object> changes) {
    try {
      for (var entry : changes.entrySet()) MapSettings.class.getField(entry.getKey()).set(settings, entry.getValue());
    } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unknown map quick setting", e); }
  }
}
