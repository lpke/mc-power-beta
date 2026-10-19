package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.Catalog;

/** Explicit JSON validation keeps saved layouts readable with the game's Gson 2.8.9. */
public final class DebugPresets {
  private DebugPresets() {}
  public static JsonArray checked(JsonElement value) {
    if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() > 32)
      throw new IllegalArgumentException("Save up to 32 F3 presets.");
    Set<String> ids = new HashSet<>();
    for (JsonElement item : value.getAsJsonArray()) {
      if (!item.isJsonObject()) throw new IllegalArgumentException("Invalid F3 preset.");
      JsonObject preset = item.getAsJsonObject();
      String id = text(preset, "id"), name = text(preset, "name");
      if (!id.matches("[A-Za-z0-9_-]{1,64}") || !ids.add(id) || name.isBlank() || name.length() > 64 || name.chars().anyMatch(Character::isISOControl))
        throw new IllegalArgumentException("Invalid F3 preset name or ID.");
      new DebugColumns(DebugOrder.read(preset.get("left")), DebugOrder.read(preset.get("right")));
      JsonObject values = preset.getAsJsonObject("values");
      if (values == null || values.size() > 128) throw new IllegalArgumentException("Invalid F3 preset settings.");
      for (var entry : values.entrySet()) {
        if (!setting(entry.getKey()) || !entry.getValue().isJsonPrimitive()) throw new IllegalArgumentException("Invalid F3 preset setting.");
        JsonPrimitive p = entry.getValue().getAsJsonPrimitive();
        if (entry.getKey().equals("power_hud:config.coordinateDisplay")) {
          if (!p.isNumber() || p.getAsBigDecimal().intValueExact() < 0 || p.getAsInt() > 2) throw new IllegalArgumentException("Invalid coordinate display.");
        } else if (entry.getKey().equals("interface.debugTextSize")) {
          if (!p.isNumber() || !Double.isFinite(p.getAsDouble()) || p.getAsDouble() < .5 || p.getAsDouble() > 3) throw new IllegalArgumentException("Invalid F3 text size.");
        } else if (entry.getKey().equals("interface.debugTextColor")) {
          if (!p.isString()) throw new IllegalArgumentException("Invalid F3 text color.");
          local.luke.power.light.LightSettings.rgb(p.getAsString());
        } else if (!p.isBoolean()) throw new IllegalArgumentException("Invalid F3 visibility.");
      }
    }
    return value.getAsJsonArray().deepCopy();
  }
  private static String text(JsonObject object, String key) {
    JsonElement value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid F3 preset " + key + ".");
    return value.getAsString();
  }
  public static boolean setting(String id) {
    return Set.of("interface.debugTextSize", "interface.debugTextColor", "interface.useFeetYCoordinates",
        "power_controls:userinterface.disableDebugEntityIdTags").contains(id) || id.startsWith("interface.debugLines.") || id.equals("entityculling:config.showF3Info")
        || id.equals("power_hud:config.coordinateDisplay")
        || Arrays.stream(DebugLines.values()).anyMatch(line -> line.setting().equals(id));
  }
  static JsonObject capture(String name, SettingsDraft draft) {
    JsonObject preset = new JsonObject();
    preset.addProperty("id", UUID.randomUUID().toString()); preset.addProperty("name", name.strip());
    preset.add("left", draft.get("interface.debugOrder").value.deepCopy());
    preset.add("right", draft.get("interface.debugRightOrder").value.deepCopy());
    JsonObject values = new JsonObject();
    draft.settings().stream().filter(s -> setting(s.id)).forEach(s -> values.add(s.id, s.value.deepCopy()));
    preset.add("values", values); return preset;
  }
  static void load(JsonObject preset, SettingsDraft draft) {
    // Check the complete payload before touching any draft value.
    JsonArray one = new JsonArray(); one.add(preset); checked(one);
    var values = preset.getAsJsonObject("values");
    for (var s : draft.settings()) if (values.has(s.id)) s.validate(values.get(s.id));
    draft.checkpoint();
    draft.get("interface.debugOrder").value = preset.get("left").deepCopy();
    draft.get("interface.debugRightOrder").value = preset.get("right").deepCopy();
    for (var s : draft.settings()) if (values.has(s.id)) s.value = values.get(s.id).deepCopy();
  }
}
