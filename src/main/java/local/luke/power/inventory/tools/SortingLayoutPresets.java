package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.io.IOException;
import java.util.Objects;

/** Inventory hotkeys open a cached draft; only an explicit editor save touches storage. */
public final class SortingLayoutPresets {
  private static boolean initialized;
  private SortingLayoutPresets() {}
  public static void initialize() {
    if (initialized) return;
    WorldSortingLayouts.initialize();
    initialized = true;
  }
  public static JsonElement snapshot() {
    if (!initialized) throw new IllegalStateException("Sorting layouts are not ready");
    var layout = WorldSortingLayouts.layout();
    return layout.has("presets") ? layout.get("presets").deepCopy() : null;
  }
  public static void replace(JsonElement expected, JsonObject next) throws IOException {
    if (!Objects.equals(snapshot(), expected)) throw new IOException("Sorting presets changed. Reopen this editor before saving.");
    var layout = WorldSortingLayouts.layout(); layout.add("presets", next.deepCopy());
    WorldSortingLayouts.replace(layout, false);
  }
  public static void queued(JsonElement expected, JsonObject next) {
    if (!Objects.equals(snapshot(), expected)) throw new IllegalStateException("Sorting presets changed. Reopen this editor.");
    SortingLayoutsData.read(next);
    var layout = WorldSortingLayouts.layout(); layout.add("presets", next.deepCopy());
    try { WorldSortingLayouts.replace(layout, true); }
    catch (IOException failure) { throw new IllegalStateException("Sorting presets could not be queued", failure); }
  }
}
