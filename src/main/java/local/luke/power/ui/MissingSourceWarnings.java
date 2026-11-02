package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicLibrarySources;

/** Consumes scanner snapshots only; each exact source path is shown once per session. */
final class MissingSourceWarnings {
  private final Set<String> shown;
  private final Deque<String> pending = new ArrayDeque<>();
  MissingSourceWarnings(Set<String> shown) { this.shown = shown; }
  void observe(Collection<MusicLibrarySources.Folder> sources) {
    Set<String> unavailable = new LinkedHashSet<>();
    for (var source : sources) if (source.status().equals("Unavailable")) unavailable.add(source.path());
    pending.removeIf(path -> !unavailable.contains(path) || shown.contains(path));
    for (String path : unavailable) if (!shown.contains(path) && !pending.contains(path)) pending.add(path);
  }
  String next() {
    String path = pending.poll();
    if (path != null) shown.add(path);
    return path;
  }
  int remaining() { return pending.size(); }
  void reset() { pending.clear(); shown.clear(); }
}
