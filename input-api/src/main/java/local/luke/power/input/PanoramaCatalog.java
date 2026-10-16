package local.luke.power.input;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stable selection IDs may share identical original image files across game versions. */
public final class PanoramaCatalog {
  public record Entry(String id, String label, String folder) {}
  private static final List<Entry> ENTRIES = load();
  private PanoramaCatalog() {}
  private static List<Entry> load() {
    try (InputStream in = PanoramaCatalog.class.getResourceAsStream("/assets/power_controls/panoramas.json")) {
      if (in == null) return List.of();
      List<Entry> entries = new ArrayList<>();
      for (JsonElement element : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
        JsonObject row = element.getAsJsonObject();
        entries.add(new Entry(row.get("id").getAsString(), row.get("label").getAsString(), row.get("folder").getAsString()));
      }
      entries.add(new Entry("glacier", "Glacier", "glacier"));
      return List.copyOf(entries);
    } catch (IOException | RuntimeException e) { throw new IllegalStateException("Cannot load panorama catalogue", e); }
  }
  public static List<Entry> entries() { return ENTRIES; }
  public static String folder(String id) {
    String key = id.equals("beta18") ? "b1_8_1" : id;
    return ENTRIES.stream().filter(e -> e.id().equals(key)).map(Entry::folder).findFirst()
        .orElse(id.matches("[A-Za-z0-9_-]{1,64}") ? id : "glacier");
  }
  public static String label(String id) {
    String key = id.equals("beta18") ? "b1_8_1" : id;
    return ENTRIES.stream().filter(e -> e.id().equals(key)).map(Entry::label).findFirst().orElse(id);
  }
}
