package local.luke.power.audio;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Per-variant official gains. Native sound-pool selection keeps equally weighted variants random. */
public final class ExtraSoundVariants {
  private static final String PREFIX = "power_beta:entity.player.attack.";
  private ExtraSoundVariants() {}

  private static final class Catalog {
    static final Map<String, Float> VOLUMES = load();
    private static Map<String, Float> load() {
      try (var input = ExtraSoundVariants.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json")) {
        var manifest = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, Float> volumes = new HashMap<>();
        Map<String, Integer> indices = new HashMap<>();
        for (var value : manifest.getAsJsonArray("assets")) {
          var asset = value.getAsJsonObject(); String event = asset.get("event").getAsString();
          if (!event.startsWith("entity.player.attack.")) continue;
          int index = indices.merge(event, 1, Integer::sum) - 1;
          var definition = manifest.getAsJsonObject("events").getAsJsonObject(event).getAsJsonArray("sounds").get(index).getAsJsonObject();
          String file = asset.get("file").getAsString();
          volumes.put(file.substring(file.indexOf("/sounds/sound/") + "/sounds/sound/".length()), definition.has("volume") ? definition.get("volume").getAsFloat() : 1);
        }
        return Map.copyOf(volumes);
      } catch (IOException | RuntimeException failure) { throw new ExceptionInInitializerError(failure); }
    }
  }

  public static float volume(String id, String filename) {
    if (id == null || !id.startsWith(PREFIX) || filename == null) return 1;
    String path = filename.replace('\\', '/'); int start = path.indexOf("entity/player/attack/");
    return start < 0 ? 1 : Catalog.VOLUMES.getOrDefault(path.substring(start), 1f);
  }
}
