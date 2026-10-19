package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;

public final class SprintBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    List<Setting> entries = new ArrayList<>();
    SprintSettings now = SprintConfig.copy(), defaults = new SprintSettings();
    for (var field : SprintSettings.class.getFields()) {
      String name = field.getName();
      var meta = Catalog.metadata("sprint." + name);
      if (meta.size() == 0)
        throw new IllegalStateException("Missing sprint setting metadata: " + name);
      Setting.Kind kind =
          field.getType() == boolean.class
              ? Setting.Kind.BOOLEAN
              : field.getType() == int.class ? Setting.Kind.INTEGER : Setting.Kind.DECIMAL;
      List<String> choices = new ArrayList<>();
      if (meta.has("choices")) {
        meta.getAsJsonArray("choices").forEach(v -> choices.add(v.getAsString()));
        kind = Setting.Kind.CHOICE;
      }
      entries.add(
          Catalog.setting(
              "sprint." + name,
              "sprint",
              "Gameplay",
              "Sprinting",
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
    session.add(new SprintBackend(), entries);
  }

  public String id() {
    return "sprint";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  private SprintSettings draft(Map<String, JsonElement> values) throws Exception {
    SprintSettings next = SprintConfig.copy();
    for (var entry : values.entrySet()) {
      var field = SprintSettings.class.getField(entry.getKey().substring(7));
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
    SprintConfig.preview(draft(values));
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    SprintConfig.save(draft(values));
  }
}
