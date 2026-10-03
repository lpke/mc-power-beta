package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Descriptions are independent of titles; empty and repeated-title tooltips stay hidden. */
public final class Tooltips {
  private static final JsonObject EXTRA = load();
  private static JsonObject load() {
    try (var in = Tooltips.class.getResourceAsStream("/assets/powerbeta/setting-help.json")) {
      if (in == null) return new JsonObject();
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (IOException e) { throw new IllegalStateException(e); }
  }
  public static String description(String id, String label, String fallback) {
    String text = EXTRA.has(id) ? EXTRA.get(id).getAsString() : fallback;
    if (text == null) return "";
    text = text.replace("Left click increases; right click decreases.", "")
        .replace("Restart required for changes to take effect", "")
        .replace("Restart required.", "").strip();
    if (text.equalsIgnoreCase(label) || text.equalsIgnoreCase(label + ".")) return "";
    if (text.startsWith(label + "\n")) text = text.substring(label.length()).strip();
    return text.replace("Power Beta defaults", "defaults").replace("Power Beta Defaults", "Defaults");
  }
}
