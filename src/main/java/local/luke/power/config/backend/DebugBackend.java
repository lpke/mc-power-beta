package local.luke.power.config.backend;

import com.google.gson.*;
import java.util.*;
import java.nio.file.Path;
import local.luke.power.config.*;
import local.luke.power.debug.DebugConfig;
import local.luke.power.storage.PowerConfig;

public final class DebugBackend implements Backend {
  public static void register(ConfigSession session) {
    session.add(new DebugBackend(), List.of(new Setting("debug.enabled", "debug", "General", "Debug shortcuts",
        "Debug shortcuts", "Enable the Debug modifier shortcuts. Edit the modifier and action keys in Controls. The mode picker remains independently available.",
        Setting.Kind.BOOLEAN, new JsonPrimitive(DebugConfig.enabled()), new JsonPrimitive(false), 0, 1, 1, List.of(), false)));
  }
  public String id() { return "debug"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  public void validate(Map<String,JsonElement> values) { for(var v:values.values()) if(!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected a debug toggle"); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String,JsonElement> values) { if(values.containsKey("debug.enabled")) DebugConfig.preview(values.get("debug.enabled").getAsBoolean()); }
  public void apply(Map<String,JsonElement> values) throws Exception { if(values.containsKey("debug.enabled")) DebugConfig.save(values.get("debug.enabled").getAsBoolean()); }
}
