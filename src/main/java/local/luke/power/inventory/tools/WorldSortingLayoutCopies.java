package local.luke.power.inventory.tools;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** World archives carry only their own layout data, even when global preferences are excluded. */
public final class WorldSortingLayoutCopies {
  private WorldSortingLayoutCopies() {}
  public static JsonObject snapshot(Path game, Collection<String> owners) throws IOException {
    return snapshot(game, owners, null);
  }
  public static JsonObject snapshot(Path game, Collection<String> owners, Path directory) throws IOException {
    Path config = game.resolve("config/power-beta.json"); safe(config);
    JsonObject document = Files.exists(config) ? JsonParser.parseString(Files.readString(config)).getAsJsonObject() : new JsonObject();
    JsonObject settings = document.has("settings") && document.get("settings").isJsonObject() ? document.getAsJsonObject("settings") : new JsonObject();
    JsonObject store = settings.has(WorldSortingLayoutsData.SECTION) ? settings.getAsJsonObject(WorldSortingLayoutsData.SECTION)
        : WorldSortingLayoutsData.migrate(settings).json();
    if (directory != null && owners.size() == 1 && store.has("format") && store.get("format").getAsString().equals("1")) {
      String id = owners.iterator().next();
      if (!store.has("worlds") || !store.get("worlds").isJsonObject() || !store.getAsJsonObject("worlds").has(id)) {
        JsonObject inherited = archived(directory, id);
        if (inherited != null) return inherited;
      }
    }
    JsonObject snapshot = new JsonObject(), worlds = new JsonObject();
    if (store.has("format")) snapshot.add("format", store.get("format").deepCopy());
    for (String id : owners) {
      JsonElement layout = store.has("worlds") && store.get("worlds").isJsonObject() ? store.getAsJsonObject("worlds").get(id) : null;
      if (layout == null) layout = store.get("legacy");
      if (layout != null) worlds.add(id, layout.deepCopy());
    }
    if (owners.isEmpty() && store.has("legacy")) worlds.add("legacy", store.get("legacy").deepCopy());
    snapshot.add("worlds", worlds);
    if (!store.has("format") || !store.get("format").isJsonPrimitive() || !store.getAsJsonPrimitive("format").isNumber()
        || !store.get("format").getAsString().equals("1")) snapshot.add("retainedStore", store.deepCopy());
    return snapshot;
  }
  public static JsonObject imported(Path directory, String identity) throws IOException {
    JsonObject snapshot = archived(directory, identity);
    if (snapshot == null) return null;
    try {
      if (!snapshot.has("format") || snapshot.get("format").getAsBigDecimal().intValueExact() != 1
          || !snapshot.has("worlds") || snapshot.getAsJsonObject("worlds").size() != 1)
        throw new IllegalArgumentException("Unsupported copied-world sorting layout. Retain power-beta-copy.json.");
      JsonObject layout = snapshot.getAsJsonObject("worlds").entrySet().iterator().next().getValue().getAsJsonObject().deepCopy();
      SortingLayoutsData.layout(layout.get("protectedSlots"), layout.get("preferredItems")); return layout;
    } catch (RuntimeException unsupported) { throw new IOException("Copied sorting layout could not be read. Retain power-beta-copy.json.", unsupported); }
  }
  private static JsonObject archived(Path directory, String identity) throws IOException {
    Path manifest = directory.resolve("power-beta-copy.json"); safe(manifest);
    if (!Files.exists(manifest, LinkOption.NOFOLLOW_LINKS)) return null;
    String original = Files.readString(manifest);
    try {
      JsonObject copy = JsonParser.parseString(original).getAsJsonObject();
      if (!copy.has("duplicateWorldId") || !identity.equals(copy.get("duplicateWorldId").getAsString()) || !copy.has("sortingLayouts")) return null;
      return copy.getAsJsonObject("sortingLayouts").deepCopy();
    } catch (RuntimeException unsupported) {
      JsonObject retained = new JsonObject(); retained.addProperty("format", -1); retained.addProperty("retainedCopyManifest", original); return retained;
    }
  }
  private static void safe(Path path) throws IOException {
    for (Path part = path; part != null; part = part.getParent()) if (Files.isSymbolicLink(part)) throw new IOException("Unsafe sorting-layout snapshot path.");
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 4 * 1024 * 1024))
      throw new IOException("Sorting-layout snapshot exceeds its supported size.");
  }
}
