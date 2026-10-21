package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.io.IOException;
import java.util.Objects;
import local.luke.power.storage.PowerConfig;

/** Inventory hotkeys open a cached draft; only an explicit editor save touches storage. */
public final class SortingLayoutPresets {
  private static JsonElement current;
  private static boolean initialized;
  private SortingLayoutPresets() {}
  public static void initialize() {
    if (initialized) return;
    current = PowerConfig.document().getAsJsonObject("settings").get("sortingLayouts");
    if (current != null) current = current.deepCopy();
    initialized = true;
  }
  public static JsonElement snapshot() {
    if (!initialized) throw new IllegalStateException("Sorting layouts are not ready");
    return current == null ? null : current.deepCopy();
  }
  public static void replace(JsonElement expected, JsonObject next) throws IOException {
    if (!Objects.equals(current, expected)) throw new IOException("Sorting presets changed. Reopen this editor before saving.");
    PowerConfig.replaceSection("sortingLayouts", expected, next);
    current = next.deepCopy();
  }
}
