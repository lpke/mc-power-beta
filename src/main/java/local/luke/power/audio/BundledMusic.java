package local.luke.power.audio;

import java.util.*;
import net.minecraft.class_267;

/** Stream immutable bundled assets directly; no disk scan or network request in the music tick. */
final class BundledMusic {
  private static final Map<String, class_267> ENTRIES = load();
  private BundledMusic() {}
  private static Map<String, class_267> load() {
    Map<String, class_267> result = new LinkedHashMap<>();
    for (var track : BuiltinMusic.TRACKS) {
      var url = BundledMusic.class.getResource("/assets/powerbeta/music/" + track.file());
      if (url == null) throw new IllegalStateException("Missing bundled music: " + track.file());
      result.put(track.id(), new class_267(track.file(), AudioResource.streaming(url)));
    }
    return Collections.unmodifiableMap(result);
  }
  static class_267 entry(String id) { return ENTRIES.get(id); }
  static List<class_267> selection(AudioSettings.MusicMode mode) {
    return BuiltinMusic.TRACKS.stream().filter(t -> t.included(mode)).map(t -> ENTRIES.get(t.id())).toList();
  }
}
