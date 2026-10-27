package local.luke.power.audio;

import java.util.*;

public final class AudioSettings {
  public boolean redstonePlacementSound = true;
  public boolean seedPlantingSounds = false;
  public boolean hoeTillingSounds = false;
  public boolean bucketSounds = false;
  public boolean lavaBucketSounds = false;
  public boolean boneMealSounds = false;
  public boolean lavaPopSounds = false;
  public boolean lavaAmbientSounds = false;
  public boolean playerFireExtinguish = false;
  public boolean waterSplashSounds = false;
  public boolean swimmingSounds = false;
  public boolean underwaterEnterSounds = false;
  public boolean underwaterExitSounds = false;
  public boolean underwaterAmbience = false;
  public boolean underwaterAdditions = false;
  public boolean underwaterRareAdditions = false;
  public boolean underwaterUltraRareAdditions = false;
  public enum DimensionMusic { CONTINUE, STOP_SPECIFIC, STOP_ALL }
  public enum MenuControlsPosition {
    MENU_TOP, MENU_BOTTOM, TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
  }
  public boolean menuControls = true;
  public boolean menuControlsMainMenu = true;
  public boolean menuControlsScrub = false;
  public MenuControlsPosition menuControlsPosition = MenuControlsPosition.MENU_BOTTOM;
  public int menuControlsOffsetX = 0, menuControlsOffsetY = 0;
  public DimensionMusic dimensionMusic = DimensionMusic.CONTINUE;
  public int gapMinSeconds = 600, gapMaxSeconds = 1200;
  public int master = 100;
  public Map<String, Integer> categories = new LinkedHashMap<>();
  public Map<String, Integer> sounds = new TreeMap<>();
  public Set<String> favourites = new LinkedHashSet<>();
  public Map<String, Integer> libraryGroupVolumes = new TreeMap<>();
  public List<String> libraryOrder = new ArrayList<>();
  public List<String> playlistOrder = new ArrayList<>();
  public String playlist = MusicPlaylist.BETA, menuPlaylist = MusicPlaylist.SAME_AS_WORLD;
  public List<MusicPlaylist> playlists = new ArrayList<>();
  public List<String> musicDirectories = new ArrayList<>();
  public boolean shuffle = true, avoidRepeats = true;
  public boolean waitBetweenTracks = true, delayQueuedTracks = false;

  /** Settings discovery retains controls for unavailable songs without querying files. */
  public List<String> musicTrackIds(Collection<String> available) {
    LinkedHashSet<String> tracks = new LinkedHashSet<>(available);
    tracks.addAll(MusicPlaylist.beta().tracks());
    for (MusicPlaylist saved : playlists) tracks.addAll(saved.tracks());
    tracks.addAll(favourites);
    sounds.keySet().stream().filter(id -> id.startsWith("music:")).forEach(tracks::add);
    return List.copyOf(tracks);
  }

  public boolean showMenuControls(boolean mainMenu) {
    return menuControls && (!mainMenu || menuControlsMainMenu);
  }

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
    MusicPlaylist.validate(playlists, playlist, menuPlaylist);
    MusicPlaylist.validateOrder(playlists, playlistOrder);
    if (menuControlsPosition == null || Math.abs((long) menuControlsOffsetX) > 4096
        || Math.abs((long) menuControlsOffsetY) > 4096)
      throw new IllegalArgumentException("Menu control offsets must be -4096 to 4096 pixels");
    MusicPlaylist.validateTrackIds(favourites);
    MusicGroups.copy(libraryGroupVolumes);
    MusicLibrarySources.validateOrder(libraryOrder);
    volume(master);
    if (gapMinSeconds < 0 || gapMaxSeconds < gapMinSeconds || gapMaxSeconds > 86400)
      throw new IllegalArgumentException("Music gaps must be 0 to 86400 seconds, with maximum at least minimum");
    if (dimensionMusic == null)
      throw new IllegalArgumentException("Music selection is incomplete");
    if (categories == null
        || sounds == null
        || musicDirectories == null) throw new IllegalArgumentException("Audio settings are incomplete");
    for (var e : categories.entrySet()) {
      if (!new AudioSettings().categories.containsKey(e.getKey()))
        throw new IllegalArgumentException("Unknown audio category");
      volume(e.getValue());
    }
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
    if (musicDirectories.size() > 32) throw new IllegalArgumentException("Use at most 32 music folders");
    for (String dir : musicDirectories)
        if (dir == null || dir.isBlank() || dir.length() > 4096 || dir.indexOf('\0') >= 0)
          throw new IllegalArgumentException("Invalid music folder");
    if (new HashSet<>(musicDirectories).size() != musicDirectories.size())
      throw new IllegalArgumentException("Music folders must be unique");
  }

  private static void volume(Integer value) {
    if (value == null || value < 0 || value > 100)
      throw new IllegalArgumentException("Volume must be 0 to 100%");
  }

  public double gain(String sound, boolean menu) {
    return gain(sound, menu, null);
  }

  double gain(String sound, boolean menu, String category) {
    return master
        / 100.0
        * categories.getOrDefault(category == null ? category(sound, menu) : category, 100)
        / 100.0
        * sounds.getOrDefault(sound, 100)
        / 100.0;
  }

  public static String category(String id, boolean interfaceSound) {
    if (id == null) return "ambient";
    id = id.substring(id.indexOf(':') + 1);
    if (id.startsWith("ambient.weather") || id.contains("thunder")) return "weather";
    if (id.startsWith("ambient") || id.startsWith("portal") || id.equals("block.lava.pop") || id.equals("block.lava.ambient")) return "ambient";
    if (id.equals("entity.generic.extinguish_fire") || id.startsWith("entity.player.") || id.equals("item.bucket.fill") || id.equals("item.bucket.fill_lava")) return "players";
    if (id.startsWith("mob.") || id.startsWith("entity.")) {
      String mob = id.substring(id.indexOf('.') + 1).split("\\.")[0];
      return Set.of("cow", "pig", "sheep", "chicken", "wolf", "squid", "generic").contains(mob)
          ? "passive"
          : "hostile";
    }
    if (id.startsWith("step.")
        || id.equals("item.crop.plant")
        || id.equals("item.hoe.till")
        || id.startsWith("item.bucket.")
        || id.equals("item.bone_meal.use")
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
