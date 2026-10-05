package local.luke.power.audio;

import java.util.*;

/** Stable group identifiers shared by playback and the library. */
public final class MusicGroups {
  private MusicGroups() {}

  public static String id(String trackId, MusicLibrary.Track custom) {
    if (custom != null)
      return "folder:" + Objects.toString(custom.path().getParent(), "Custom tracks");
    var builtin = BuiltinMusic.find(trackId);
    return builtin == null ? "Other tracks" : builtin.group();
  }

  public static Map<String, Integer> copy(Map<String, Integer> volumes) {
    if (volumes == null || volumes.size() > 8192)
      throw new IllegalArgumentException("Invalid music group volumes");
    Map<String, Integer> result = new TreeMap<>();
    for (var entry : volumes.entrySet()) {
      String id = entry.getKey();
      Integer volume = entry.getValue();
      if (id == null
          || id.isBlank()
          || id.length() > 4096
          || id.codePoints().anyMatch(Character::isISOControl)
          || volume == null
          || volume < 0
          || volume > 100) throw new IllegalArgumentException("Group volumes must be 0 to 100%");
      if (volume != 100) result.put(id, volume);
    }
    return result;
  }

  public static float gain(Map<String, Integer> volumes, String group) {
    return volumes.getOrDefault(group, 100) / 100f;
  }
}
