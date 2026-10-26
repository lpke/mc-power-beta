package local.luke.power.audio;

import java.util.*;

/** An ordered saved selection. Loading copies it into the independent working selection. */
public final class MusicPlaylist {
  public static final String SAME_AS_WORLD = "@world", DEFAULT = "@default";
  private final String id, name;
  private final List<String> tracks;
  private final Map<String, Integer> groupVolumes;

  public MusicPlaylist(String id, String name, List<String> tracks, Map<String, Integer> groupVolumes) {
    if (id == null || !id.matches("[a-zA-Z0-9_-]{1,64}"))
      throw new IllegalArgumentException("Invalid playlist identifier");
    if (name == null || name.isBlank() || name.length() > 80
        || name.codePoints().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Use a playlist name of 1 to 80 characters");
    validateTracks(tracks);
    this.id = id;
    this.name = name.strip();
    this.tracks = new ArrayList<>(tracks);
    this.groupVolumes = MusicGroups.copy(groupVolumes);
  }

  public String id() { return id; }
  public String name() { return name; }
  public List<String> tracks() { return Collections.unmodifiableList(tracks); }
  public Map<String, Integer> groupVolumes() { return Collections.unmodifiableMap(groupVolumes); }
  public boolean includes(String track) { return tracks.contains(track); }

  public static void validateTracks(List<String> tracks) {
    if (tracks == null) throw new IllegalArgumentException("Invalid playlist tracks");
    validateTrackIds(tracks);
    if (tracks.size() > 8192 || tracks.size() != new HashSet<>(tracks).size())
      throw new IllegalArgumentException("Playlist tracks must be unique");
  }

  public static void validateTrackIds(Collection<String> tracks) {
    if (tracks == null || tracks.size() > 8192 || tracks.stream().anyMatch(id -> id == null
        || !id.startsWith("music:") || id.length() > 512
        || id.codePoints().anyMatch(Character::isISOControl)))
      throw new IllegalArgumentException("Invalid music selection");
  }

  public static void validate(List<MusicPlaylist> playlists, String world, String menu) {
    if (playlists == null || playlists.size() > 128 || world == null || menu == null)
      throw new IllegalArgumentException("Invalid music playlists");
    Set<String> ids = new HashSet<>();
    for (MusicPlaylist p : playlists) {
      if (p == null) throw new IllegalArgumentException("Invalid music playlist");
      new MusicPlaylist(p.id, p.name, p.tracks, p.groupVolumes);
      if (!ids.add(p.id)) throw new IllegalArgumentException("Duplicate playlist identifier");
    }
    if (!world.isEmpty() && !ids.contains(world)
        || !menu.equals(SAME_AS_WORLD) && !menu.equals(DEFAULT) && !ids.contains(menu))
      throw new IllegalArgumentException("Selected music playlist is missing");
  }

  private static MusicPlaylist find(AudioSettings settings, String id) {
    return settings.playlists.stream().filter(p -> p.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Music playlist is missing"));
  }

  public static void load(AudioSettings settings, String id) {
    if (id.isEmpty()) {
      settings.playlist = "";
      settings.playlistTracks = new ArrayList<>();
      return;
    }
    MusicPlaylist p = find(settings, id);
    MusicPlaylist checked = new MusicPlaylist(p.id, p.name, p.tracks, p.groupVolumes);
    settings.playlistTracks = new ArrayList<>(checked.tracks);
    settings.groupVolumes = new TreeMap<>(checked.groupVolumes);
    settings.playlist = checked.id;
  }

  public static void loadMenu(AudioSettings settings, String id) {
    if (SAME_AS_WORLD.equals(id) || DEFAULT.equals(id)) {
      settings.menuPlaylist = id;
      settings.menuPlaylistTracks = new ArrayList<>();
      settings.menuGroupVolumes = new TreeMap<>();
      return;
    }
    MusicPlaylist p = find(settings, id);
    MusicPlaylist checked = new MusicPlaylist(p.id, p.name, p.tracks, p.groupVolumes);
    settings.menuPlaylistTracks = new ArrayList<>(checked.tracks);
    settings.menuGroupVolumes = new TreeMap<>(checked.groupVolumes);
    settings.menuPlaylist = checked.id;
  }
}
