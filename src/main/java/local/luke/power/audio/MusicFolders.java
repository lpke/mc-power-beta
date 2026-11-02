package local.luke.power.audio;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Imported folders use exact saved path strings. */
public final class MusicFolders {
  public static final int MAX_FOLDERS = 32;

  public static void validateIdentities(List<String> folders, Map<String, String> identities) {
    if (folders == null || identities == null || identities.size() > MAX_FOLDERS
        || !folders.containsAll(identities.keySet()))
      throw new IllegalArgumentException("Music source identities must belong to imported folders");
    for (String identity : identities.values()) {
      if (identity == null || identity.isBlank() || identity.length() > 4096
          || identity.codePoints().anyMatch(Character::isISOControl)
          || !Path.of(identity).isAbsolute())
        throw new IllegalArgumentException("Invalid music source identity");
    }
  }

  /** One explicit folder edit. Stable song identities leave playlists and queues untouched. */
  public static AudioSettings relink(Path game, AudioSettings current, String oldPath, String newPath)
      throws IOException {
    current.validate();
    if (!current.musicDirectories.contains(oldPath)) throw new IOException("Folder changed. Reopen Relink.");
    Path replacement = canonical(game, newPath);
    if (!Files.isDirectory(replacement, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(replacement))
      throw new IOException("Replacement folder unavailable.");
    for (String folder : current.musicDirectories)
      if (!folder.equals(oldPath) && canonical(game, folder).equals(replacement))
        throw new IOException("That folder is already imported.");
    String path = replacement.toString();
    AudioSettings next = new Gson().fromJson(new Gson().toJson(current), AudioSettings.class);
    next.musicDirectories.set(next.musicDirectories.indexOf(oldPath), path);
    String identity = next.musicSourceIdentities.remove(oldPath);
    if (identity == null) {
      // A nested import can already inherit song IDs from a previously relinked parent.
      var sources = current.musicSourceIdentities.keySet().stream()
          .map(folder -> new MusicLibrary.SourceFolder(folder, canonical(game, folder), true, List.of()))
          .toList();
      identity = MusicLibrary.identity(canonical(game, oldPath), sources, current.musicSourceIdentities).toString();
    }
    next.musicSourceIdentities.put(path, identity);
    String oldGroup = MusicLibrarySources.id(oldPath), newGroup = MusicLibrarySources.id(path);
    next.libraryOrder.replaceAll(group -> group.equals(oldGroup) ? newGroup : group);
    next.libraryOrder = new ArrayList<>(new LinkedHashSet<>(next.libraryOrder));
    Integer gain = next.libraryGroupVolumes.remove(oldGroup);
    if (gain != null) next.libraryGroupVolumes.put(newGroup, gain);
    next.validate();
    return next;
  }

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
