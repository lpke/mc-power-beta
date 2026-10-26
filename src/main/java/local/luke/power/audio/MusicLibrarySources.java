package local.luke.power.audio;

import java.nio.file.Path;
import java.util.*;

/** Source ownership prepared from scanner observations without filesystem access. */
public final class MusicLibrarySources {
  private MusicLibrarySources() {}

  public record Folder(String id, String path, String label, boolean enabled, String status,
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

  private record Root(String path, String label, Path root, boolean enabled,
      String status, List<String> warnings) {}

  public static String id(String path) { return "source:" + path; }

  /** Most-specific enabled root owns each track; equal roots retain configured order. */
  public static Snapshot build(Path game, List<String> configured, Set<String> disabled,
      MusicLibrary.Scan scan, boolean recursive) {
    Map<String, MusicLibrary.SourceFolder> observed = new LinkedHashMap<>();
    for (var source : scan.sources()) observed.putIfAbsent(source.path(), source);
    List<Root> roots = new ArrayList<>();
    for (String path : configured) {
      Path lexical = null;
      try { lexical = MusicLibrary.resolve(game, path).toAbsolutePath().normalize(); }
      catch (RuntimeException ignored) { /* Invalid saved paths remain manageable. */ }
      String label = lexical == null ? path : Objects.toString(lexical.getFileName(), lexical.toString());
      boolean enabled = !disabled.contains(path);
      var source = enabled ? observed.get(path) : null;
      String status = !enabled ? "Off" : source == null ? "Scanning" : source.available() ? "Ready" : "Unavailable";
      roots.add(new Root(path, label,
          !enabled || source != null && !source.available() ? null : source == null ? lexical : source.root(),
          enabled, status, source == null ? List.of() : source.warnings()));
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
        boolean contains = recursive ? file.startsWith(root.root()) : root.root().equals(file.getParent());
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
      folders.add(new Folder(group, root.path(), root.label(), root.enabled(), status, members, root.warnings()));
    }
    return new Snapshot(folders, owners, tracks);
  }
}
