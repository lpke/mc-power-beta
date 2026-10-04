package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.light.*;
import local.luke.power.storage.PowerConfig;

public final class LightBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    LightBackend backend = new LightBackend();
    LightSettings current = LightConfig.copy(), defaults = new LightSettings();
    List<Setting> entries = new ArrayList<>();
    add(entries, current, defaults, "enabled", "Light overlay", Setting.Kind.BOOLEAN, 0, 1, 1,
        "Show light numbers on nearby block tops. No scans run while off. Toggle with F7 or your assigned key.");
    add(entries, current, defaults, "radius", "Horizontal radius", Setting.Kind.INTEGER, 2, 24, 1,
        "Distance from the camera in blocks. Smaller ranges use less work; unloaded chunks are skipped.");
    add(entries, current, defaults, "verticalRange", "Vertical range", Setting.Kind.INTEGER, 1, 16, 1,
        "Blocks above and below the camera's feet to inspect. Larger ranges take longer to refresh.");
    add(entries, current, defaults, "lightSource", "Light source", Setting.Kind.CHOICE, 0, 1, 1,
        "Block light ignores daylight, useful for lighting builds. Combined includes current daylight and weather.");
    add(entries, current, defaults, "spawnableOnly", "Spawnable surfaces only", Setting.Kind.BOOLEAN, 0, 1, 1,
        "Require Beta's solid spawn support and two clear blocks above. Light alone does not guarantee or prevent every mob spawn.");
    add(entries, current, defaults, "greenFrom", "Colour threshold", Setting.Kind.INTEGER, 0, 16, 1,
        "Values below this use the low-light colour; values at or above use the high-light colour. Beta's usual cutoff is 8.");
    add(entries, current, defaults, "lowColor", "Low-light colour", Setting.Kind.TEXT, 0, 0, 1,
        "RGB hex colour for values below the threshold, such as #FF5555 for red.");
    add(entries, current, defaults, "highColor", "High-light colour", Setting.Kind.TEXT, 0, 0, 1,
        "RGB hex colour for values at or above the threshold, such as #55FF55 for green.");
    add(entries, current, defaults, "textSize", "Number size", Setting.Kind.DECIMAL, .2, .9, .05,
        "Width of a two-digit number in blocks. Numbers sit flat on the top face and remain hidden behind walls.");
    session.add(backend, entries);
  }

  private static void add(List<Setting> entries, LightSettings current, LightSettings defaults,
      String field, String label, Setting.Kind kind, double min, double max, double step, String help)
      throws Exception {
    var f = LightSettings.class.getField(field);
    entries.add(new Setting("lightOverlay." + field, "lightOverlay", "Building", "Light overlay",
        label, help, kind, Catalog.JSON.toJsonTree(f.get(current)), Catalog.JSON.toJsonTree(f.get(defaults)),
        min, max, step, field.equals("lightSource") ? List.of("Block light", "Combined light") : List.of(), false));
  }

  public String id() { return "lightOverlay"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private LightSettings draft(Map<String, JsonElement> values) throws Exception {
    LightSettings next = LightConfig.copy();
    for (var e : values.entrySet()) {
      var f = LightSettings.class.getField(e.getKey().substring("lightOverlay.".length()));
      f.set(next, Catalog.JSON.fromJson(e.getValue(), f.getType()));
    }
    next.validate();
    return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception { LightConfig.preview(draft(values)); }
  public void apply(Map<String, JsonElement> values) throws Exception { LightConfig.save(draft(values)); }
}
