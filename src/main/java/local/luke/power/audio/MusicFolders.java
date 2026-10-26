package local.luke.power.audio;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Imported folders use exact saved path strings. */
public final class MusicFolders {
  public static final int MAX_FOLDERS = 32;

  public static Path canonical(Path game, String folder) {
    Path path = MusicLibrary.resolve(game, folder).toAbsolutePath().normalize();
    try { return path.toRealPath(); }
    catch (IOException ignored) { return path; }
  }

  /** Validate the whole batch before changing the draft. Existing entries retain their path identity. */
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
  public static JsonArray encode(List<String> paths) {
    JsonArray result = new JsonArray();
    paths.forEach(result::add);
    return result;
  }

  public static List<String> decode(JsonElement value) {
    if (value == null || !value.isJsonArray()) throw new IllegalArgumentException("Use a list of music folders");
    List<String> paths = new ArrayList<>();
    for (JsonElement entry : value.getAsJsonArray()) {
      if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString())
        throw new IllegalArgumentException("Folder paths must be text");
      String path = entry.getAsString();
      if (paths.contains(path)) throw new IllegalArgumentException("Music folders must be unique");
      paths.add(path);
    }
    return List.copyOf(paths);
  }
}
