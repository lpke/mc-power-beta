package local.luke.power.audio;

import java.util.*;

public final class AudioSettings {
  public enum MusicMode {
    VANILLA,
    ADD,
    REPLACE
  }

  public int master = 100;
  public Map<String, Integer> categories = new LinkedHashMap<>();
  public Map<String, Integer> sounds = new TreeMap<>();
  public Set<String> disabledTracks = new TreeSet<>();
  public MusicMode musicMode = MusicMode.VANILLA;
  public List<String> musicDirectories = new ArrayList<>();
  public List<String> menuDirectories = new ArrayList<>();
  public Set<String> disabledMusicDirectories = new LinkedHashSet<>();
  public Set<String> disabledMenuDirectories = new LinkedHashSet<>();
  public boolean recursive = false, shuffle = true, avoidRepeats = true;
  public boolean waitBetweenTracks = true, delayQueuedTracks = false;

  public AudioSettings() {
    for (String key :
        List.of(
            "blocks",
            "hostile",
            "passive",
            "players",
            "weather",
            "ambient",
            "interface",
            "records")) categories.put(key, 100);
  }

  public void validate() {
    volume(master);
    if (categories == null
        || sounds == null
        || musicDirectories == null
        || menuDirectories == null
        || disabledMusicDirectories == null
        || disabledMenuDirectories == null
        || musicMode == null) throw new IllegalArgumentException("Audio settings are incomplete");
    for (var e : categories.entrySet()) {
      if (!new AudioSettings().categories.containsKey(e.getKey()))
        throw new IllegalArgumentException("Unknown audio category");
      volume(e.getValue());
    }
    if (disabledTracks == null || disabledTracks.size() > 4096 || disabledTracks.stream().anyMatch(id -> id == null || !id.startsWith("music:") || id.length() > 512))
      throw new IllegalArgumentException("Invalid excluded tracks");
    if (sounds.size() > 4096) throw new IllegalArgumentException("Too many sound overrides");
    for (var e : sounds.entrySet()) {
      String id = e.getKey();
      if (id == null
          || id.isBlank()
          || id.length() > 512
          || id.codePoints().anyMatch(Character::isISOControl))
        throw new IllegalArgumentException("Invalid sound identifier");
      volume(e.getValue());
    }
    for (List<String> dirs : List.of(musicDirectories, menuDirectories)) {
      if (dirs.size() > 32) throw new IllegalArgumentException("Use at most 32 music folders");
      for (String dir : dirs)
        if (dir == null || dir.isBlank() || dir.length() > 4096 || dir.indexOf('\0') >= 0)
          throw new IllegalArgumentException("Invalid music folder");
    }
    if (!musicDirectories.containsAll(disabledMusicDirectories)
        || !menuDirectories.containsAll(disabledMenuDirectories))
      throw new IllegalArgumentException("Disabled music folders must remain in the folder list");
  }

  private static void volume(Integer value) {
    if (value == null || value < 0 || value > 100)
      throw new IllegalArgumentException("Volume must be 0 to 100%");
  }

  public double gain(String sound, boolean menu) {
    return master
        / 100.0
        * categories.getOrDefault(category(sound, menu), 100)
        / 100.0
        * sounds.getOrDefault(sound, 100)
        / 100.0;
  }

  public static String category(String id, boolean interfaceSound) {
    if (id == null) return "ambient";
    id = id.substring(id.indexOf(':') + 1);
    if (id.startsWith("ambient.weather") || id.contains("thunder")) return "weather";
    if (id.startsWith("ambient") || id.startsWith("portal")) return "ambient";
    if (id.startsWith("mob.") || id.startsWith("entity.")) {
      String mob = id.substring(id.indexOf('.') + 1).split("\\.")[0];
      return Set.of("cow", "pig", "sheep", "chicken", "wolf", "squid").contains(mob)
          ? "passive"
          : "hostile";
    }
    if (id.startsWith("step.")
        || id.startsWith("dig.")
        || id.startsWith("tile.")
        || id.startsWith("fire.")
        || id.startsWith("liquid.")
        || id.startsWith("note.")
        || id.startsWith("random.chest")) return "blocks";
    if (id.startsWith("records.") || id.startsWith("streaming.")) return "records";
    if (id.equals("random.click") || interfaceSound) return "interface";
    return "players";
  }
}
