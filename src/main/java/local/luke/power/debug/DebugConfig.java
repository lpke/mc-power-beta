package local.luke.power.debug;

import com.google.gson.JsonPrimitive;
import local.luke.power.config.DefaultBases;
import local.luke.power.storage.PowerConfig;

public final class DebugConfig {
  private static Boolean preview;
  private DebugConfig() {}
  public static boolean enabled() {
    if (preview != null) return preview;
    var section = PowerConfig.section("debug");
    preview = section.has("enabled") ? section.get("enabled").getAsBoolean()
        : DefaultBases.value("debug.enabled", new JsonPrimitive(false)).getAsBoolean();
    return preview;
  }
  public static void preview(boolean value) { preview = value; }
  public static void save(boolean value) throws java.io.IOException {
    var section = PowerConfig.section("debug"); section.addProperty("enabled", value);
    PowerConfig.save("debug", section); preview = value;
  }
}
