package local.luke.power.audio;

import java.nio.file.Path;
import java.util.*;

/** Source ownership prepared from scanner observations without filesystem access. */
public final class MusicLibrarySources {
  private MusicLibrarySources() {}

  public record Folder(String id, String path, String label, String status,
      List<String> trackIds, List<String> warnings) {
    public Folder {
      trackIds = List.copyOf(trackIds);
      warnings = List.copyOf(warnings);
    }
  }

  public record Snapshot(List<Folder> folders, Map<String, String> trackSources,
      List<MusicLibrary.Track> tracks) {
    public Snapshot {
      folders = List.copyOf(folders);
      trackSources = Map.copyOf(trackSources);
      tracks = List.copyOf(tracks);
    }
  }

  private record Root(String path, String label, Path root,
      String status, List<String> warnings) {}

  public static String id(String path) { return "source:" + path; }

  /** Only presentation order changes; source ownership retains configured precedence. */
  public static List<String> order(AudioSettings settings) {
    LinkedHashSet<String> available = new LinkedHashSet<>();
    settings.musicDirectories.forEach(path -> available.add(id(path)));
    available.addAll(BuiltinMusic.GROUPS);
    List<String> ordered = new ArrayList<>();
    for (String group : settings.libraryOrder) if (available.remove(group)) ordered.add(group);
    ordered.addAll(available);
    return List.copyOf(ordered);
  }

  public static void validateOrder(List<String> order) {
    if (order == null || order.size() > 8192 || order.size() != new HashSet<>(order).size()
        || order.stream().anyMatch(id -> id == null || id.isBlank() || id.length() > 4103
            || id.codePoints().anyMatch(Character::isISOControl)
            || !BuiltinMusic.GROUPS.contains(id) && (!id.startsWith("source:") || id.substring(7).isBlank())))
      throw new IllegalArgumentException("Invalid music library order");
  }

  /** Most-specific imported root owns each track; equal roots retain configured order. */
  public static Snapshot build(Path game, List<String> configured,
      MusicLibrary.Scan scan) {
    Map<String, MusicLibrary.SourceFolder> observed = new LinkedHashMap<>();
    for (var source : scan.sources()) observed.putIfAbsent(source.path(), source);
    List<Root> roots = new ArrayList<>();
    for (String path : configured) {
      Path lexical = null;
      try { lexical = MusicLibrary.resolve(game, path).toAbsolutePath().normalize(); }
      catch (RuntimeException ignored) { /* Invalid saved paths remain manageable. */ }
      String label = lexical == null ? path : Objects.toString(lexical.getFileName(), lexical.toString());
      var source = observed.get(path);
      String status = source == null ? "Scanning" : source.available() ? "Ready" : "Unavailable";
      roots.add(new Root(path, label,
          source != null && !source.available() ? null : source == null ? lexical : source.root(),
          status, source == null ? List.of() : source.warnings()));
    }
    Set<String> covered = new HashSet<>();
    Map<String, String> owners = new LinkedHashMap<>();
    Map<String, List<String>> ids = new LinkedHashMap<>();
    List<MusicLibrary.Track> tracks = new ArrayList<>();
    for (var track : scan.tracks()) {
      if (owners.containsKey(track.id())) continue;
      Root owner = null;
      Path file = track.path().toAbsolutePath().normalize();
      for (var root : roots) {
        if (root.root() == null) continue;
        boolean contains = file.startsWith(root.root());
        if (contains) {
          covered.add(id(root.path()));
          if (owner == null || root.root().getNameCount() > owner.root().getNameCount()) owner = root;
        }
      }
      if (owner == null) continue;
      String group = id(owner.path());
      owners.put(track.id(), group);
      ids.computeIfAbsent(group, ignored -> new ArrayList<>()).add(track.id());
      tracks.add(track);
    }
    List<Folder> folders = new ArrayList<>();
    for (var root : roots) {
      String group = id(root.path());
      List<String> members = ids.getOrDefault(group, List.of());
      String status = root.status();
      if (status.equals("Ready")) {
        boolean incomplete = root.warnings().stream().anyMatch(warning ->
            warning.startsWith("Music scan reached") || warning.startsWith("Music library reached")
                || warning.startsWith("Folder scan incomplete") || warning.startsWith("Could not finish reading"));
        if (incomplete) status = "Incomplete";
        else if (members.isEmpty()) status = covered.contains(group) ? "Shared" : "Empty";
      }
      folders.add(new Folder(group, root.path(), root.label(), status, members, root.warnings()));
    }
    return new Snapshot(folders, owners, tracks);
  }
}
