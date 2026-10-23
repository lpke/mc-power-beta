package local.luke.power.storage;

import com.google.gson.*;

/** Current-format validation, independent of the release version. */
public final class ConfigVersions {
  public static final int CURRENT = 7;
  private ConfigVersions() {}

  public static int version(JsonObject root) {
    try {
      JsonPrimitive version = root.getAsJsonPrimitive("schemaVersion");
      if (!version.isNumber()) throw new IllegalArgumentException();
      return version.getAsBigDecimal().intValueExact();
    } catch (RuntimeException e) { throw new IllegalArgumentException("Missing or invalid schemaVersion. Supported configuration version: 7. Upgrade older files with Power Beta 4.4.1 first.", e); }
  }
  public static JsonObject convert(JsonObject source) {
    int version = version(source);
    if (version != CURRENT)
      throw new IllegalArgumentException("Configuration version " + version + " is unsupported. Supported version: 7. Upgrade older files with Power Beta 4.4.1 first, or use the release that saved this file; original file is preserved.");
    if (!source.has("settings") || !source.get("settings").isJsonObject())
      throw new IllegalArgumentException("Configuration settings must be an object.");
    JsonObject root = source.deepCopy();
    JsonObject settings = root.getAsJsonObject("settings");
    // Creation templates have their own review boundary. Preserve unsupported documents
    // verbatim so they cannot block unrelated preferences or be consumed without consent.
    validateDebugLayout(settings);
    return root;
  }
  private static void validateDebugLayout(JsonObject settings) {
    JsonObject ui = settings.has("interface") ? settings.getAsJsonObject("interface") : new JsonObject();
    var left = ui.has("debugOrder") ? local.luke.power.ui.DebugOrder.read(ui.get("debugOrder")) : local.luke.power.ui.DebugOrder.defaults();
    var right = ui.has("debugRightOrder") ? local.luke.power.ui.DebugOrder.read(ui.get("debugRightOrder")) : local.luke.power.ui.DebugColumns.defaultRight();
    new local.luke.power.ui.DebugColumns(left, right);
    if (ui.has("debugTextSize")) {
      var n = ui.get("debugTextSize");
      if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isNumber() || !Double.isFinite(n.getAsDouble()) || n.getAsDouble() < .5 || n.getAsDouble() > 3)
        throw new IllegalArgumentException("F3 text size must be between 0.5 and 3. Original settings preserved.");
    }
    if (ui.has("debugLines")) {
      if (!ui.get("debugLines").isJsonObject()) throw new IllegalArgumentException("F3 visibility must be an object.");
      for (var e : ui.getAsJsonObject("debugLines").entrySet())
        if (!e.getKey().matches("[A-Za-z0-9_.-]{1,96}") || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean())
          throw new IllegalArgumentException("Invalid F3 visibility setting. Original settings preserved.");
    }
  }
}
