package local.luke.power.audio;

import java.util.*;

/** Presets are immutable snapshots. Loading copies exclusions into the live settings. */
public final class MusicPreset {
  private final String id, name;
  private final Set<String> excluded;

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Set<String> excluded() {
    return Collections.unmodifiableSet(excluded);
  }

  public MusicPreset(String id, String name, Set<String> excluded) {
    if (id == null || !id.matches("[a-zA-Z0-9_-]{1,64}"))
      throw new IllegalArgumentException("Invalid preset identifier");
    if (name == null
        || name.isBlank()
        || name.length() > 80
        || name.codePoints().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Use a preset name of 1 to 80 characters");
    this.id = id;
    this.name = name.strip();
    validateTracks(excluded);
    this.excluded = new TreeSet<>(excluded);
  }

  public static void validateTracks(Set<String> tracks) {
    if (tracks == null
        || tracks.size() > 8192
        || tracks.stream()
            .anyMatch(
                id ->
                    id == null
                        || !id.startsWith("music:")
                        || id.length() > 512
                        || id.codePoints().anyMatch(Character::isISOControl)))
      throw new IllegalArgumentException("Invalid music selection");
  }

  public static void validate(List<MusicPreset> presets, String selected) {
    if (presets == null || presets.size() > 128 || selected == null)
      throw new IllegalArgumentException("Invalid music presets");
    Set<String> ids = new HashSet<>();
    for (MusicPreset p : presets) {
      if (p == null) throw new IllegalArgumentException("Invalid music preset");
      new MusicPreset(p.id, p.name, p.excluded);
      if (!ids.add(p.id)) throw new IllegalArgumentException("Duplicate preset identifier");
    }
    if (!selected.isEmpty() && !ids.contains(selected))
      throw new IllegalArgumentException("Selected music preset is missing");
  }

  public static void load(AudioSettings settings, String id) {
    if (id.isEmpty()) {
      settings.preset = "";
      return;
    }
    MusicPreset selected =
        settings.presets.stream()
            .filter(p -> p.id.equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Music preset is missing"));
    settings.disabledTracks = new TreeSet<>(selected.excluded);
    settings.preset = selected.id;
  }
}
