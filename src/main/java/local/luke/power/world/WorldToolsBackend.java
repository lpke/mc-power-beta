package local.luke.power.world;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class WorldToolsBackend implements Backend {
  public static void register(ConfigSession session) {
    WorldToolsSettings current = WorldToolsConfig.copy();
    session.add(new WorldToolsBackend(), List.of(
        setting("backup", "World backup button", current.backup, "Show Backup in a saved world's Edit menu. Backups include the save and matching map and recovery files."),
        setting("duplicate", "Duplicate world button", current.duplicate, "Show Duplicate in a saved world's Edit menu. The copy has a new world identity; recovery history stays with the original."),
        setting("recreate", "Re-create world button", current.recreate, "Show Re-create from seed in a saved world's Edit menu. Review its seed, difficulty and game rules before creating fresh terrain.")));
  }
  private static Setting setting(String key, String label, boolean value, String help) {
    return new Setting("worldTools." + key, "worldTools", "Interface", "World menu", label, help,
        Setting.Kind.BOOLEAN, new JsonPrimitive(value), new JsonPrimitive(false), 0, 1, 1, List.of(), false);
  }
  public String id() { return "worldTools"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private WorldToolsSettings draft(Map<String, JsonElement> values) {
    WorldToolsSettings next = WorldToolsConfig.copy();
    for (var entry : values.entrySet()) {
      if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Choose On or Off");
      boolean value = entry.getValue().getAsBoolean();
      switch (entry.getKey()) {
        case "worldTools.backup" -> next.backup = value;
        case "worldTools.duplicate" -> next.duplicate = value;
        case "worldTools.recreate" -> next.recreate = value;
        default -> throw new IllegalArgumentException("Unknown world-menu setting");
      }
    }
    return next;
  }
  public void validate(Map<String, JsonElement> values) { draft(values); }
  public void apply(Map<String, JsonElement> values) throws Exception { WorldToolsConfig.save(draft(values)); }
}
