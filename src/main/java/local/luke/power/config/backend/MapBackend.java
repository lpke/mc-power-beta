package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;

/** The normal Options editor owns map drafts, live previews and rollback. */
public final class MapBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    List<Setting> entries = new ArrayList<>();
    MapSettings now = MapConfig.copy(), defaults = new MapSettings();
    for (var field : MapSettings.class.getFields()) {
      String name = field.getName();
      var meta = Catalog.metadata("maps." + name);
      if (meta.size() == 0)
        throw new IllegalStateException("Missing map setting metadata: " + name);
      Setting.Kind kind =
          field.getType() == boolean.class
              ? Setting.Kind.BOOLEAN
              : field.getType().isArray() ? Setting.Kind.LIST
                  : field.getType() == double.class ? Setting.Kind.DECIMAL : Setting.Kind.INTEGER;
      List<String> choices = new ArrayList<>();
      if (meta.has("choices")) {
        meta.getAsJsonArray("choices").forEach(v -> choices.add(v.getAsString()));
        kind = Setting.Kind.CHOICE;
      }
      entries.add(
          Catalog.setting(
              "maps." + name,
              "maps",
              "Maps",
              "Minimap",
              Catalog.words(name),
              "",
              kind,
              Catalog.JSON.toJsonTree(field.get(now)),
              Catalog.JSON.toJsonTree(field.get(defaults)),
              0,
              1,
              1,
              choices,
              false));
    }
    entries.add(
        new Setting(
            "maps.hudEditor",
            "maps",
            "Map",
            "Minimap",
            "Minimap layout and appearance",
            "Open the HUD editor's minimap controls for size, position, shape, opacity and"
                + " information.",
            Setting.Kind.TEXT,
            new com.google.gson.JsonPrimitive(""),
            new com.google.gson.JsonPrimitive(""),
            0,
            0,
            1,
            List.of(),
            false));
    session.add(new MapBackend(), entries);
  }

  public String id() {
    return "maps";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  private MapSettings draft(Map<String, JsonElement> values) throws Exception {
    MapSettings next = MapConfig.copy();
    for (var entry : values.entrySet()) {
      if (entry.getKey().equals("maps.hudEditor")) continue;
      var field = MapSettings.class.getField(entry.getKey().substring(5));
      field.set(next, Catalog.JSON.fromJson(entry.getValue(), field.getType()));
    }
    next.validate();
    return next;
  }

  public void validate(Map<String, JsonElement> values) throws Exception {
    draft(values);
  }

  public boolean previews(Setting setting) {
    return true;
  }

  public void preview(Map<String, JsonElement> values) throws Exception {
    MapConfig.preview(draft(values));
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    MapConfig.save(draft(values));
  }
}
