package local.luke.power.input;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stable selection IDs may share identical original image files across game versions. */
public final class PanoramaCatalog {
  public record Entry(String id, String label, String folder, String scene) {
    public Entry(String id, String label, String folder) { this(id, label, folder, id); }
  }
  public static final String DIRT = "b1_7_3";
  private static final List<Entry> ALL = load();
  private static final List<Entry> ENTRIES = unique();
  private PanoramaCatalog() {}
  private static List<Entry> load() {
    try (InputStream in = PanoramaCatalog.class.getResourceAsStream("/assets/power_controls/panoramas.json")) {
      if (in == null) return List.of();
      List<Entry> entries = new ArrayList<>();
      for (JsonElement element : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
        JsonObject row = element.getAsJsonObject();
        String id = row.get("id").getAsString();
        entries.add(new Entry(id, row.get("label").getAsString(), row.get("folder").getAsString(),
            row.has("scene") ? row.get("scene").getAsString() : id));
      }
      entries.add(new Entry("glacier", "Glacier", "glacier", "glacier"));
      return List.copyOf(entries);
    } catch (IOException | RuntimeException e) { throw new IllegalStateException("Cannot load panorama catalogue", e); }
  }
  private static List<Entry> unique() {
    Map<String, Entry> scenes = new LinkedHashMap<>();
    for (Entry entry : ALL) scenes.putIfAbsent(entry.scene(),
        new Entry(entry.id(), entry.label(), folder(entry.id()), entry.scene()));
    return List.copyOf(scenes.values());
  }
  public static List<Entry> entries() { return ENTRIES; }
  public static String folder(String id) {
    // Release 1.0 fixes the classic scene's purple seam. Keep its Beta name and saved aliases.
    String key = switch (id) {
      case "beta18", "b1_8_1", "official_b1_8_1" -> "1_0";
      default -> id;
    };
    return ALL.stream().filter(e -> e.id().equals(key)).map(Entry::folder).findFirst()
        .orElse(id.matches("[A-Za-z0-9_-]{1,64}") ? id : "glacier");
  }
  public static String label(String id) {
    String key = scene(id);
    return ENTRIES.stream().filter(e -> e.id().equals(key)).map(Entry::label).findFirst().orElse(id);
  }
  public static String scene(String id) {
    String key = id.equals("beta18") ? "b1_8_1" : id;
    return ALL.stream().filter(e -> e.id().equals(key) || e.folder().equals(key)).map(Entry::scene).findFirst().orElse(key);
  }
}
