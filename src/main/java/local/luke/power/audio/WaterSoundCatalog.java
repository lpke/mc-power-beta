package local.luke.power.audio;

import com.google.gson.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable official variant gains and streaming flags, read once outside rendering. */
final class WaterSoundCatalog {
  record Variant(URL url, String filename, float volume, boolean stream, int durationTicks) {}
  private static final Map<String, List<Variant>> EVENTS = load();

  private static Map<String, List<Variant>> load() {
    try (var input = WaterSoundCatalog.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json")) {
      var manifest = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8)).getAsJsonObject();
      Map<String, List<Variant>> events = new HashMap<>();
      for (var value : manifest.getAsJsonArray("assets")) {
        var asset = value.getAsJsonObject(); String event = asset.get("event").getAsString();
        if (!event.startsWith("ambient.underwater.")) continue;
        String file = asset.get("file").getAsString();
        List<Variant> variants = events.computeIfAbsent("power_beta:" + event, ignored -> new ArrayList<>());
        var entry = manifest.getAsJsonObject("events").getAsJsonObject(event).getAsJsonArray("sounds").get(variants.size());
        var definition = entry.isJsonObject() ? entry.getAsJsonObject() : new JsonObject();
        variants.add(new Variant(AudioResource.streaming(Objects.requireNonNull(WaterSoundCatalog.class.getResource("/" + file))),
            file, definition.has("volume") ? definition.get("volume").getAsFloat() : 1,
            definition.has("stream") && definition.get("stream").getAsBoolean(), asset.get("durationTicks").getAsInt()));
      }
      events.replaceAll((key, value) -> List.copyOf(value));
      return Map.copyOf(events);
    } catch (IOException | RuntimeException e) { throw new ExceptionInInitializerError(e); }
  }

  static Variant choose(String id, Random random) {
    var variants = EVENTS.get(id);
    return variants == null ? null : variants.get(random.nextInt(variants.size()));
  }
}
