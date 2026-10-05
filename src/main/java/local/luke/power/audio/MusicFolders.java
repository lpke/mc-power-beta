package local.luke.power.audio;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Folder switches preserve paths and track identities. Existing string-list settings still load. */
public final class MusicFolders {
  public static final int MAX_FOLDERS = 32;

  public static Path canonical(Path game, String folder) {
    Path path = MusicLibrary.resolve(game, folder).toAbsolutePath().normalize();
    try { return path.toRealPath(); }
    catch (IOException ignored) { return path; }
  }

  /** Validate the whole batch before changing the draft. Existing entries keep their On/Off state. */
  public static List<String> additions(Path game, List<String> existing, Collection<String> selected,
      boolean childFolders) throws IOException {
    Set<Path> known = new HashSet<>();
    existing.forEach(path -> known.add(canonical(game, path)));
    Set<String> additions = new LinkedHashSet<>();
    ScanBudget budget = new ScanBudget();
    for (String folder : selected) {
      if (folder.isBlank()) throw new IOException("Enter a folder path.");
      Path root = MusicLibrary.resolve(game, folder);
      if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(root))
        throw new IOException("Folder unavailable: " + folder);
      List<Path> candidates;
      if (childFolders) {
        try (var stream = Files.list(root)) {
          candidates = stream.limit(MusicLibrary.MAX_VISITED + 1L).sorted().toList();
        }
      } else candidates = List.of(root);
      for (Path candidate : candidates) {
        budget.visit();
        if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) continue;
        Path canonical = candidate.toRealPath();
        if (known.contains(canonical)) continue;
        if (childFolders && !hasAudio(candidate, budget)) continue;
        known.add(canonical);
        additions.add(canonical.toString());
        if (existing.size() + additions.size() > MAX_FOLDERS)
          throw new IOException("Use at most 32 folders. Nothing was added.");
      }
    }
    return List.copyOf(additions);
  }

  private static final class ScanBudget {
    private int visited;
    void visit() throws IOException {
      if (++visited > MusicLibrary.MAX_VISITED)
        throw new IOException("Too many files to scan. Select fewer folders. Nothing was added.");
    }
  }

  private static boolean hasAudio(Path directory, ScanBudget budget) throws IOException {
    if (!Files.isReadable(directory)) return false;
    try (var stream = Files.list(directory)) {
      var files = stream.iterator();
      while (files.hasNext()) {
        budget.visit();
        if (MusicLibrary.audioFile(files.next())) return true;
      }
    } catch (java.nio.file.AccessDeniedException | SecurityException ignored) { }
    return false;
  }
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
