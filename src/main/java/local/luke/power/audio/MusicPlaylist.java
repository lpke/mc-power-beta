package local.luke.power.audio;

import java.util.*;

/** An immutable saved playlist. Playback reads the selected object directly. */
public final class MusicPlaylist {
  public static final String SAME_AS_WORLD = "@world", BETA = "beta", FAVORITES = "favorites";
  private final String id, name;
  private final List<String> tracks;

  public MusicPlaylist(String id, String name, List<String> tracks) {
    if (id == null || !id.matches("[a-zA-Z0-9_-]{1,64}"))
      throw new IllegalArgumentException("Invalid playlist identifier");
    if (name == null || name.isBlank() || name.length() > 80
        || name.codePoints().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException("Use a playlist name of 1 to 80 characters");
    validateTracks(tracks);
    this.id = id;
    this.name = name.strip();
    this.tracks = new ArrayList<>(tracks);
  }

  public String id() { return id; }
  public String name() { return name; }
  public List<String> tracks() { return Collections.unmodifiableList(tracks); }
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

  private static final MusicPlaylist BETA_PLAYLIST = new MusicPlaylist(BETA, "Beta 1.7.3", BuiltinMusic.VANILLA);

  public static MusicPlaylist beta() { return BETA_PLAYLIST; }

  public static MusicPlaylist favorites(AudioSettings settings) {
    return new MusicPlaylist(FAVORITES, "Favorites", List.copyOf(settings.favourites));
  }

  public static boolean reserved(String id) { return BETA.equals(id) || FAVORITES.equals(id); }

  public static List<MusicPlaylist> all(AudioSettings settings) {
    List<MusicPlaylist> result = new ArrayList<>();
    result.add(beta());
    result.add(favorites(settings));
    result.addAll(settings.playlists);
    Map<String, MusicPlaylist> byId = new LinkedHashMap<>();
    result.forEach(p -> byId.put(p.id(), p));
    List<MusicPlaylist> ordered = new ArrayList<>();
    for (String id : settings.playlistOrder) {
      MusicPlaylist saved = byId.remove(id);
      if (saved != null) ordered.add(saved);
    }
    ordered.addAll(byId.values());
    return Collections.unmodifiableList(ordered);
  }

  public static void validate(List<MusicPlaylist> playlists, String world, String menu) {
    if (playlists == null || playlists.size() > 128 || world == null || menu == null)
      throw new IllegalArgumentException("Invalid music playlists");
    Set<String> ids = new HashSet<>();
    ids.add(BETA);
    ids.add(FAVORITES);
    for (MusicPlaylist p : playlists) {
      if (p == null) throw new IllegalArgumentException("Invalid music playlist");
      new MusicPlaylist(p.id, p.name, p.tracks);
      if (!ids.add(p.id)) throw new IllegalArgumentException("Duplicate or reserved playlist identifier");
    }
    if (!ids.contains(world) || !menu.equals(SAME_AS_WORLD) && !ids.contains(menu))
      throw new IllegalArgumentException("Selected music playlist is missing");
  }

  public static void validateOrder(List<MusicPlaylist> playlists, List<String> order) {
    if (order == null || order.size() > 130 || new HashSet<>(order).size() != order.size())
      throw new IllegalArgumentException("Invalid playlist catalog order");
    Set<String> ids = new HashSet<>(List.of(BETA, FAVORITES));
    playlists.forEach(p -> ids.add(p.id()));
    if (!ids.containsAll(order)) throw new IllegalArgumentException("Playlist order contains a missing playlist");
  }

  public static MusicPlaylist find(AudioSettings settings, String id) {
    if (BETA.equals(id)) return beta();
    if (FAVORITES.equals(id)) return favorites(settings);
    MusicPlaylist p = settings.playlists.stream().filter(value -> value.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Music playlist is missing"));
    return p;
  }

  public static MusicPlaylist world(AudioSettings settings) { return find(settings, settings.playlist); }

  public static MusicPlaylist menu(AudioSettings settings) {
    return SAME_AS_WORLD.equals(settings.menuPlaylist) ? world(settings) : find(settings, settings.menuPlaylist);
  }

  public static String selectionId(AudioSettings settings, boolean inMenu) {
    return inMenu && !SAME_AS_WORLD.equals(settings.menuPlaylist) ? settings.menuPlaylist : settings.playlist;
  }

  /** Selection order is also the non-shuffled playback order. */
  public static List<String> rotation(AudioSettings settings, boolean inMenu) {
    return find(settings, selectionId(settings, inMenu)).tracks();
  }

  /** Playback lookups read validated settings without constructing a Favorites snapshot. */
  public static boolean contains(AudioSettings settings, String selection, String track) {
    String id = SAME_AS_WORLD.equals(selection) ? settings.playlist : selection;
    return FAVORITES.equals(id) ? settings.favourites.contains(track) : find(settings, id).includes(track);
  }

  public static void load(AudioSettings settings, String id) {
    MusicPlaylist p = find(settings, id);
    new MusicPlaylist(p.id, p.name, p.tracks);
    settings.playlist = id;
  }

  public static void loadMenu(AudioSettings settings, String id) {
    if (!SAME_AS_WORLD.equals(id)) {
      MusicPlaylist p = find(settings, id);
      new MusicPlaylist(p.id, p.name, p.tracks);
    }
    settings.menuPlaylist = id;
  }
}
