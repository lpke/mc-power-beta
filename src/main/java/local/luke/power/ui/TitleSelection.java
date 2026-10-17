package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import local.luke.power.config.backend.LogoBackend;

/** Menu clicks apply one complete selection; the shared durable worker saves its snapshot. */
public final class TitleSelection {
  private static String error = "";
  private TitleSelection() {}
  public static String error() { return error; }
  static JsonObject design(Collection<Setting> settings) {
    JsonObject result = new JsonObject();
    for (Setting setting : settings) if (TitleTiles.DESIGN_KEYS.contains(setting.id)) result.add(setting.id, setting.value.deepCopy());
    return result;
  }
  static int index(int style, String selected, JsonArray presets, JsonObject current) {
    if (style > 0) return presets.size() + style;
    for (int i = 0; i < presets.size(); i++) {
      JsonObject preset = presets.get(i).getAsJsonObject();
      if (preset.get("id").getAsString().equals(selected) && preset.getAsJsonObject("values").equals(current)) return i + 1;
    }
    return 0;
  }
  public static void cycle(int direction) {
    if (direction != -1 && direction != 1) throw new IllegalArgumentException("Invalid title cycle direction");
    try {
      ConfigSession session = new ConfigSession(); LogoBackend backend = LogoBackend.create(session);
      Map<String, Setting> settings = new LinkedHashMap<>(); session.settings().forEach(s -> settings.put(s.id, s));
      JsonArray presets = settings.get("logo.presets").value.getAsJsonArray();
      new LogoBackend().validate(Map.of("logo.presets", presets));
      JsonObject current = design(session.settings());
      int style = settings.get("logo.style").value.getAsInt();
      int index = index(style, settings.get("logo.selectedPreset").value.getAsString(), presets, current);
      Setting base = settings.get("logo.retroBase");
      if (index == 0 || base.value.getAsJsonObject().size() == 0) base.value = current.deepCopy();
      int next = Math.floorMod(index + direction, presets.size() + settings.get("logo.style").choices.size());
      JsonObject chosen = null;
      settings.get("logo.selectedPreset").value = new JsonPrimitive("");
      if (next == 0) chosen = base.value.getAsJsonObject();
      else if (next <= presets.size()) {
        JsonObject preset = presets.get(next - 1).getAsJsonObject(); chosen = preset.getAsJsonObject("values");
        settings.get("logo.selectedPreset").value = preset.get("id").deepCopy();
      }
      settings.get("logo.style").value = new JsonPrimitive(next <= presets.size() ? 0 : next - presets.size());
      if (chosen != null) chosen.entrySet().forEach(e -> settings.get(e.getKey()).value = e.getValue().deepCopy());
      for (Setting setting : settings.values()) setting.validate(setting.value);
      Map<String, JsonElement> values = new LinkedHashMap<>();
      settings.forEach((id, setting) -> values.put(id, setting.value.deepCopy()));
      backend.selectDeferred(values); error = "";
    } catch (Exception failure) {
      error = "Could not save title selection. Check settings recovery.";
      PowerBeta.LOG.error("Could not change title selection", failure);
    }
  }
}
