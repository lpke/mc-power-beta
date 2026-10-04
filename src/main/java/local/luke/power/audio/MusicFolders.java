package local.luke.power.audio;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Folder switches preserve paths and track identities. Existing string-list settings still load. */
public final class MusicFolders {
  public record Selection(List<String> paths, Set<String> disabled) {
    public Selection {
      paths = List.copyOf(paths);
      disabled = Set.copyOf(disabled);
    }
  }

  public static JsonArray encode(List<String> paths, Set<String> disabled) {
    JsonArray result = new JsonArray();
    for (String path : paths) {
      if (!disabled.contains(path)) result.add(path);
      else {
        JsonObject entry = new JsonObject();
        entry.addProperty("path", path);
        entry.addProperty("enabled", false);
        result.add(entry);
      }
    }
    return result;
  }

  public static Selection decode(JsonElement value) {
    if (!value.isJsonArray()) throw new IllegalArgumentException("Use a list of music folders");
    List<String> paths = new ArrayList<>();
    Set<String> disabled = new LinkedHashSet<>();
    for (JsonElement entry : value.getAsJsonArray()) {
      boolean enabled = true;
      JsonElement path = entry;
      if (entry.isJsonObject()) {
        JsonObject object = entry.getAsJsonObject();
        path = object.get("path");
        JsonElement flag = object.get("enabled");
        if (flag == null || !flag.isJsonPrimitive() || !flag.getAsJsonPrimitive().isBoolean())
          throw new IllegalArgumentException("Folder state must be On or Off");
        enabled = flag.getAsBoolean();
      }
      if (path == null || !path.isJsonPrimitive() || !path.getAsJsonPrimitive().isString())
        throw new IllegalArgumentException("Folder paths must be text");
      String text = path.getAsString();
      if (!paths.contains(text)) paths.add(text);
      if (!enabled) disabled.add(text);
    }
    return new Selection(paths, disabled);
  }

  /** Evaluate membership once per settings change, not for each audio tick or track. */
  public static List<MusicLibrary.Track> enabledTracks(Path game, MusicLibrary.Scan scan,
      List<String> folders, Set<String> disabled, boolean recursive) {
    Set<Path> roots = new HashSet<>();
    for (String folder : folders) {
      if (disabled.contains(folder)) continue;
      try {
        Path root = MusicLibrary.resolve(game, folder);
        if (!Files.isSymbolicLink(root)) roots.add(root.toRealPath());
      } catch (IOException | RuntimeException ignored) { /* Unavailable folders stay in settings. */ }
    }
    return scan.tracks().stream().filter(track -> roots.stream().anyMatch(root ->
        recursive ? track.path().startsWith(root) : root.equals(track.path().getParent()))).toList();
  }
}
