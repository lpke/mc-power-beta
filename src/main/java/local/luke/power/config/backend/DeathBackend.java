package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.*;
import local.luke.power.storage.PowerConfig;

public final class DeathBackend implements Backend {
  public static void register(ConfigSession session) {
    DeathSettings value = DeathConfig.copy();
    session.add(new DeathBackend(), List.of(
        entry("enabled", "Death records", "Save your death location, cause and lost items in singleplayer. Use /death for the latest death. Disabling keeps existing records. Recovery requires cheats and creative mode.", value.enabled, false),
        entry("announceLocation", "Death location in chat", "Print the coordinates, dimension, cause and short death ID when you die. Records are saved even when this notice is off.", value.announceLocation, true),
        entry("announceItems", "Lost items in chat", "Print the lost inventory and equipment when you die. You can also use /death items [id].", value.announceItems, true)));
  }
  private static Setting entry(String key, String label, String help, boolean value, boolean defaults) {
    return new Setting("death." + key, "death", "Commands", "Death records", label, help,
        Setting.Kind.BOOLEAN, new JsonPrimitive(value), new JsonPrimitive(defaults), 0, 1, 1, List.of(), false);
  }
  public String id() { return "death"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private DeathSettings draft(Map<String, JsonElement> values) throws Exception {
    DeathSettings next = DeathConfig.copy();
    for (var entry : values.entrySet()) {
      if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isBoolean())
        throw new IllegalArgumentException("Choose On or Off");
      DeathSettings.class.getField(entry.getKey().substring(6)).setBoolean(next, entry.getValue().getAsBoolean());
    }
    return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public void apply(Map<String, JsonElement> values) throws Exception { DeathConfig.save(draft(values)); }
}
