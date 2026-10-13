package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.hud.*;
import local.luke.power.storage.PowerConfig;

public final class InfoHudBackend implements Backend {
  private static final String PREFIX = "infoHud.";
  private static final List<String> POSITIONS = List.of("Top left", "Top centre", "Top right", "Middle left",
      "Centre", "Middle right", "Bottom left", "Bottom centre", "Bottom right");

  public static void register(ConfigSession session) throws Exception {
    var entries = new ArrayList<Setting>();
    var current = InfoHudConfig.current();
    var defaults = new InfoHudSettings();
    entries.add(setting("enabled", "Show custom HUD info", "Show the enabled clusters during gameplay.",
        Setting.Kind.BOOLEAN, new JsonPrimitive(current.enabled), new JsonPrimitive(false), 0, 1, 1, List.of()));
    entries.add(setting("selectedCluster", "Edit cluster", "Choose one of four independent clusters to edit. All enabled clusters remain visible.",
        Setting.Kind.CHOICE, new JsonPrimitive(current.selectedCluster), new JsonPrimitive(0), 0, 3, 1,
        List.of("Cluster 1", "Cluster 2", "Cluster 3", "Cluster 4")));
    for (int i = 0; i < InfoHudSettings.CLUSTERS; i++) for (var field : InfoHudSettings.Cluster.class.getFields()) {
      String name = field.getName();
      boolean offset = name.startsWith("offset");
      String label = switch (name) {
        case "enabled" -> "Show this cluster";
        case "position" -> "Screen position";
        case "offsetX" -> "Horizontal offset";
        case "offsetY" -> "Vertical offset";
        case "textSize" -> "Text size";
        case "textColor" -> "Text colour";
        case "opacity" -> "Text opacity";
        case "hideWithDebug" -> "Hide when F3 is open";
        case "day" -> "Day count";
        case "chunk" -> "Chunk coordinates";
        case "fps" -> "Frame rate";
        default -> Catalog.words(name);
      };
      String help = "Cluster " + (i + 1) + ". " + switch (name) {
        case "enabled" -> "Show this cluster when custom HUD info is on.";
        case "position" -> "Anchor the cluster to a screen edge or centre. Each line aligns to this anchor.";
        case "offsetX" -> "Move right with positive values, left with negative values. Uses GUI pixels.";
        case "offsetY" -> "Move down with positive values, up with negative values. Uses GUI pixels.";
        case "textSize" -> "Text scale relative to the normal HUD font. 1 is the normal size.";
        case "textColor" -> "Choose the text colour. Opacity controls transparency separately.";
        case "opacity" -> "Text visibility from transparent at 0% to solid at 100%.";
        case "hideWithDebug" -> "Hide this cluster while the F3 debug menu is open.";
        case "coordinates" -> "Show the player's block coordinates, using foot height.";
        case "direction" -> "Show the direction the player faces and its coordinate axis.";
        case "biome" -> "Show the biome at the player.";
        case "timeOfDay" -> "Show the world's daylight time with AM or PM, including frozen daylight.";
        case "day" -> "Show elapsed daylight days. Day zero is the world's first day.";
        case "chunk" -> "Show the player's chunk coordinates.";
        default -> "Show frames per second.";
      };
      Setting.Kind kind = field.getType() == boolean.class ? Setting.Kind.BOOLEAN : name.equals("position") ? Setting.Kind.CHOICE
          : field.getType() == String.class ? Setting.Kind.TEXT : field.getType() == double.class ? Setting.Kind.DECIMAL : Setting.Kind.INTEGER;
      entries.add(setting("clusters." + i + "." + name, label, help, kind,
          Catalog.JSON.toJsonTree(field.get(current.clusters[i])), Catalog.JSON.toJsonTree(field.get(defaults.clusters[i])),
          offset ? -4096 : name.equals("textSize") ? .5 : 0,
          offset ? 4096 : name.equals("textSize") ? 3 : name.equals("opacity") ? 100 : name.equals("position") ? 8 : 1,
          name.equals("textSize") ? .1 : 1, name.equals("position") ? POSITIONS : List.of()));
    }
    session.add(new InfoHudBackend(), entries);
  }
  private static Setting setting(String name, String label, String help, Setting.Kind kind, JsonElement value,
      JsonElement defaults, double min, double max, double step, List<String> choices) {
    return new Setting(PREFIX + name, "infoHud", "Interface", "Custom HUD info", label, help,
        kind, value, defaults, min, max, step, choices, false);
  }
  public static boolean selected(Setting setting, int selected) {
    return !setting.id.startsWith(PREFIX + "clusters.") || setting.id.startsWith(PREFIX + "clusters." + selected + ".");
  }
  public String id() { return "infoHud"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private InfoHudSettings draft(Map<String, JsonElement> values) throws Exception {
    var next = InfoHudConfig.copy();
    for (var entry : values.entrySet()) {
      String path = entry.getKey().substring(PREFIX.length());
      Object target = next;
      if (path.startsWith("clusters.")) {
        String[] parts = path.split("\\.");
        target = next.clusters[Integer.parseInt(parts[1])]; path = parts[2];
      }
      var field = target.getClass().getField(path);
      field.set(target, Catalog.JSON.fromJson(entry.getValue(), field.getType()));
    }
    next.validate(); return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception { InfoHudConfig.preview(draft(values)); }
  public void apply(Map<String, JsonElement> values) throws Exception { InfoHudConfig.save(draft(values)); }
}
