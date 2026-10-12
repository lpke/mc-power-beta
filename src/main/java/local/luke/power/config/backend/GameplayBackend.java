package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.storage.PowerConfig;

public final class GameplayBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    List<Setting> entries = new ArrayList<>();
    var current = GameplayConfig.copy();
    var defaults = new GameplaySettings();
    for (var field : GameplaySettings.class.getFields()) {
      String id = "gameplay." + field.getName();
      entries.add(Catalog.setting(id, "gameplay", "Gameplay", "Dropped items", Catalog.words(field.getName()), "",
          field.getType() == boolean.class ? Setting.Kind.BOOLEAN : Setting.Kind.INTEGER,
          Catalog.JSON.toJsonTree(field.get(current)), Catalog.JSON.toJsonTree(field.get(defaults)),
          0, 1, 1, List.of(), false));
    }
    session.add(new GameplayBackend(), entries);
  }
  public String id() { return "gameplay"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private GameplaySettings draft(Map<String, JsonElement> values) throws Exception {
    var settings = GameplayConfig.copy();
    for (var entry : values.entrySet()) {
      var field = GameplaySettings.class.getField(entry.getKey().substring(9));
      field.set(settings, Catalog.JSON.fromJson(entry.getValue(), field.getType()));
    }
    settings.validate();
    return settings;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public void apply(Map<String, JsonElement> values) throws Exception { GameplayConfig.save(draft(values)); }
}
