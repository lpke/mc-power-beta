package local.luke.power.ui;

import java.util.*;
import local.luke.power.config.Setting;

/** Presentation groups and the explicit boundary of a position-only reset. */
final class HudEditorSections {
  private static final List<String> GROUPS = List.of("Visibility", "Layout", "Position", "Appearance",
      "Terrain", "Entity radar", "Waypoints", "Overlays", "Item names", "Chat history", "Coordinates", "Information", "World information", "Culling statistics", "Music");
  private static final Set<String> OFFSETS = Set.of("power_compat:config.overlayAdditionsYOffset",
      "entityculling:config.f3InfoYOffset", "power_environment:config.MUSIC_CONFIG.overlayForMusicInDebugYOffset");
  private static final Set<String> NATIVE_POSITION = Set.of("horizontalPosition", "verticalPosition",
      "horizontalPositionOffset", "verticalPositionOffset", "putStatusBarIconsBelowHotbar", "putOverlayMessagesBelowHotbar");
  private static final Set<String> ANCHOR_POSITION = Set.of("position", "offsetX", "offsetY");
  private HudEditorSections() {}
  private static String field(Setting s) { return s.id.substring(s.id.lastIndexOf('.') + 1); }
  static boolean position(Setting s) {
    if (s.id.startsWith("maps.")) return Set.of("corner", "offsetX", "offsetY").contains(field(s));
    return (s.id.equals("interface.debugOrder") || s.id.equals("interface.debugRightOrder")) || OFFSETS.contains(s.id)
        || s.id.startsWith(HudPreview.NATIVE) && NATIVE_POSITION.contains(field(s))
        || (s.id.startsWith("infoHud.clusters.") || s.id.startsWith("activeTweaks.")) && ANCHOR_POSITION.contains(field(s));
  }
  static String group(Setting s) {
    String id = s.id, field = field(s);
    if (id.startsWith("maps.")) {
      if (position(s)) return "Position";
      if (field.equals("hideWithDebug")) return "Visibility";
      if (Set.of("coordinates", "biome", "clock", "compass", "directionTextSize", "belowTextSize").contains(field)) return "Information";
      if (Set.of("colourMode", "heightShading", "slopeShading", "caveLighting").contains(field)) return "Terrain";
      if (Set.of("radar", "players", "hostile", "animals", "items", "otherEntities", "entityNames", "entityHeights", "radarHeight", "radarDimming", "radarDimDistance").contains(field)) return "Entity radar";
      if (field.startsWith("waypoint") || field.startsWith("minimap") || field.startsWith("destinationArrow")) return "Waypoints";
      if (Set.of("chunkGrid", "slimeChunks", "lightOverlay", "footsteps", "pathColor", "pathOpacity").contains(field)) return "Overlays";
      return "Appearance";
    }
    if (HudEditorSettings.itemNames(s)) return "Item names";
    if (s.group.equals("Chat")) return "Chat history";
    if (Set.of(HudEditorSettings.HUD + "disableCrosshair", HudEditorSettings.HUD + "drawXboxXAndYButtons", HudEditorSettings.TOAST).contains(id)) return "Visibility";
    if (id.equals("interface.debugAxes")) return "Appearance";
    if (id.equals("interface.debugOrder")) return "Layout";
    for (var line : DebugLines.values()) if (line.setting().equals(id)) return line.culling() ? "Culling statistics" : "Information";
    if (id.equals("power_controls:userinterface.disableDebugEntityIdTags")) return "Appearance";
    if (id.startsWith("entityculling:") || id.startsWith("interface.debugLines.culling")) return "Culling statistics";
    if (id.contains("MUSIC_CONFIG.overlayForMusicInDebug")) return "Music";
    if (id.equals("interface.useFeetYCoordinates") || id.equals("power_hud:config.coordinateDisplay")) return "Coordinates";
    if (id.equals(HudEditorSettings.ENTRY) || field.startsWith("put")) return "Layout";
    if (position(s)) return "Position";
    if (Set.of("textColor", "textSize", "opacity", "debugTextColor", "debugTextSize").contains(field)) return "Appearance";
    if (Set.of("enabled", "enableVisibility", "hideWithDebug", "showVersionTextIngame").contains(field)) return "Visibility";
    return s.group.equals("Debug (F3) info") ? "World information" : "Information";
  }
  static List<Setting> order(Collection<Setting> values) {
    return values.stream().sorted(Comparator.comparingInt((Setting s) -> GROUPS.indexOf(group(s)))
        .thenComparingInt(HudEditorSections::lineOrder)).toList();
  }
  private static int lineOrder(Setting s) {
    for (var line : DebugLines.values()) if (line.setting().equals(s.id)) return line.ordinal();
    return -1;
  }
}
