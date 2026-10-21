package local.luke.power.subtitles;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class SubtitlesBackend implements Backend {
  private static final String PREFIX = "subtitles.";
  private static final List<String> POSITIONS = List.of("Top left", "Top center", "Top right", "Middle left", "Center", "Middle right", "Bottom left", "Bottom center", "Bottom right");
  public static void register(ConfigSession session) throws ReflectiveOperationException {
    List<Setting> settings = new ArrayList<>(); var current = SubtitleConfig.current(); var defaults = new SubtitleSettings();
    for (var field : SubtitleSettings.class.getFields()) {
      String name = field.getName();
      String label = switch (name) {
        case "enabled" -> "Subtitles";
        case "position" -> "Screen position";
        case "offsetX" -> "Horizontal offset";
        case "offsetY" -> "Vertical offset";
        case "textSize" -> "Text size";
        case "backgroundOpacity" -> "Background opacity";
        case "displaySeconds" -> "Display duration";
        case "hideWithDebug" -> "Hide when F3 is open";
        case "showInInventory" -> "Show in inventories";
        default -> throw new IllegalArgumentException("Unknown subtitle setting");
      };
      String help = switch (name) {
        case "enabled" -> "Show captions for nearby game sounds, including when audio is muted. Direction arrows follow the audio listener, including freecam.";
        case "position" -> "Anchor the subtitle rows to an edge or center of the screen. Rows grow upward from the first caption. The default position stays above Active tweaks.";
        case "offsetX" -> "Move subtitles right with positive values, left with negative values. Uses GUI pixels.";
        case "offsetY" -> "Move subtitles down with positive values, up with negative values. Uses GUI pixels.";
        case "textSize" -> "Scale the native font and arrows together. 1 is normal size.";
        case "backgroundOpacity" -> "Dark background visibility, from transparent at 0% to solid at 100%.";
        case "displaySeconds" -> "Seconds before a sound caption expires. The modern default is 3 seconds; repeats refresh the matching position.";
        case "hideWithDebug" -> "Hide subtitles while F3 is open. Their timers continue while hidden.";
        default -> "Keep subtitles visible over inventory and container screens.";
      };
      Setting.Kind kind = field.getType() == boolean.class ? Setting.Kind.BOOLEAN : name.equals("position") ? Setting.Kind.CHOICE
          : field.getType() == double.class ? Setting.Kind.DECIMAL : Setting.Kind.INTEGER;
      boolean offset = name.startsWith("offset");
      settings.add(new Setting(PREFIX + name, "subtitles", "Interface", "Subtitles", label, help, kind,
          Catalog.JSON.toJsonTree(field.get(current)), Catalog.JSON.toJsonTree(field.get(defaults)),
          offset ? -4096 : name.equals("textSize") ? .5 : name.equals("displaySeconds") ? 1 : 0,
          offset ? 4096 : name.equals("textSize") ? 3 : name.equals("displaySeconds") ? 15 : name.equals("backgroundOpacity") ? 100 : name.equals("position") ? 8 : 1,
          field.getType() == double.class ? .1 : 1, name.equals("position") ? POSITIONS : List.of(), false));
    }
    session.add(new SubtitlesBackend(), settings);
  }
  public String id() { return "subtitles"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  public static SubtitleSettings draft(Map<String, JsonElement> values, SubtitleSettings source) throws ReflectiveOperationException {
    SubtitleSettings next = Catalog.JSON.fromJson(Catalog.JSON.toJson(source), SubtitleSettings.class);
    for (var entry : values.entrySet()) {
      if (!entry.getKey().startsWith(PREFIX)) throw new IllegalArgumentException("Unknown subtitle setting");
      var field = SubtitleSettings.class.getField(entry.getKey().substring(PREFIX.length()));
      field.set(next, Catalog.JSON.fromJson(entry.getValue(), field.getType()));
    }
    next.validate(); return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values, SubtitleConfig.current()); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception { SubtitleConfig.preview(draft(values, SubtitleConfig.current())); }
  public void apply(Map<String, JsonElement> values) throws Exception { SubtitleConfig.save(draft(values, SubtitleConfig.current())); }
}
