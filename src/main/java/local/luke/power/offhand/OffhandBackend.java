package local.luke.power.offhand;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class OffhandBackend implements Backend {
  public static void register(ConfigSession session) {
    session.add(new OffhandBackend(), List.of(new Setting("offhand.enabled", "offhand", "Gameplay", "Inventory",
        "Offhand", "Use a second hand in singleplayer. Disabling keeps its item in an extraction-only inventory slot.",
        Setting.Kind.BOOLEAN, new JsonPrimitive(OffhandConfig.current().enabled), new JsonPrimitive(true),
        0, 1, 1, List.of(), false)));
  }
  public String id() { return "offhand"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private OffhandSettings draft(Map<String, JsonElement> values) {
    OffhandSettings next = OffhandConfig.copy();
    for (var entry : values.entrySet()) {
      if (!entry.getKey().equals("offhand.enabled") || !entry.getValue().isJsonPrimitive()
          || !entry.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Choose On or Off");
      next.enabled = entry.getValue().getAsBoolean();
    }
    return next;
  }
  public void validate(Map<String, JsonElement> values) { draft(values); }
  public void apply(Map<String, JsonElement> values) throws Exception { OffhandConfig.save(draft(values)); }
}
