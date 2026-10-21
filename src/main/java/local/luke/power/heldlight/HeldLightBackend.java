package local.luke.power.heldlight;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;

public final class HeldLightBackend implements Backend {
  public static void register(ConfigSession session) {
    session.add(new HeldLightBackend(), List.of(new Setting("heldLight.enabled", "heldLight", "Video", "Lighting",
        "Held item light", "Light nearby surfaces while holding a luminous block or lava bucket in either hand.",
        Setting.Kind.BOOLEAN, new JsonPrimitive(HeldLightConfig.current().enabled), new JsonPrimitive(false),
        0, 1, 1, List.of(), false)));
  }
  public String id() { return "heldLight"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private HeldLightSettings draft(Map<String, JsonElement> values) {
    HeldLightSettings next = HeldLightConfig.copy();
    for (var entry : values.entrySet()) {
      if (!entry.getKey().equals("heldLight.enabled") || !entry.getValue().isJsonPrimitive()
          || !entry.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Choose On or Off");
      next.enabled = entry.getValue().getAsBoolean();
    }
    return next;
  }
  public void validate(Map<String, JsonElement> values) { draft(values); }
  public boolean previews(Setting setting) { return setting.id.equals("heldLight.enabled"); }
  public void preview(Map<String, JsonElement> values) { HeldLightConfig.preview(draft(values)); }
  public void apply(Map<String, JsonElement> values) throws Exception { HeldLightConfig.save(draft(values)); }
}
