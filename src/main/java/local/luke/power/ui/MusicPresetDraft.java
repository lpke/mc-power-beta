package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicPlaylist;

/** An isolated ordered editor; unavailable entries stay in the draft until explicitly removed. */
public final class MusicPresetDraft {
  private final String id, originalName;
  private final boolean creating, readOnly;
  private final List<String> original, tracks, available;
  private List<String> cachedTracks;
  private Set<String> cachedMembership;
  private boolean cachedOrderChanged;

  public MusicPresetDraft(MusicPlaylist playlist) { this(playlist, List.of()); }

  public MusicPresetDraft(MusicPlaylist playlist, Collection<String> available) {
    this(playlist, available, playlist == null);
  }

  public MusicPresetDraft(MusicPlaylist playlist, Collection<String> available, boolean creating) {
    this(playlist, available, creating, false);
  }

  private MusicPresetDraft(MusicPlaylist playlist, Collection<String> available, boolean creating, boolean readOnly) {
    boolean builtin = !readOnly && playlist != null && MusicPlaylist.BETA.equals(playlist.id());
    this.readOnly = readOnly;
    this.creating = creating || builtin;
    id = playlist == null || builtin ? UUID.randomUUID().toString() : playlist.id();
    originalName = playlist == null ? "New playlist" : builtin ? "Beta copy" : playlist.name();
    original = playlist == null ? List.of() : List.copyOf(playlist.tracks());
    tracks = new ArrayList<>(original);
    this.available = List.copyOf(new LinkedHashSet<>(available));
  }

  public static MusicPresetDraft readOnly(MusicPlaylist playlist) {
    return new MusicPresetDraft(Objects.requireNonNull(playlist), List.of(), false, true);
  }

  public String id() { return id; }
  public String name() { return originalName; }
  public boolean readOnly() { return readOnly; }
  public boolean favorites() { return MusicPlaylist.FAVORITES.equals(id); }
  public MusicPlaylist original() { return new MusicPlaylist(id, originalName, original); }

  public static String copyName(String source, Collection<String> existing) {
    Set<String> names = new HashSet<>();
    existing.forEach(name -> names.add(name.toLowerCase(Locale.ROOT)));
    for (int number = 1; ; number++) {
      String suffix = number == 1 ? " copy" : " copy " + number;
      int end = Math.min(source.length(), 80 - suffix.length());
      if (end > 0 && end < source.length() && Character.isHighSurrogate(source.charAt(end - 1))) end--;
      String name = source.substring(0, end).stripTrailing() + suffix;
      if (!names.contains(name.toLowerCase(Locale.ROOT))) return name;
    }
  }
  public boolean creating() { return creating; }
  public boolean included(String id) { refreshSnapshot(); return cachedMembership.contains(id); }
  public List<String> tracks() { refreshSnapshot(); return cachedTracks; }
  private void refreshSnapshot() {
    if (cachedTracks != null) return;
    cachedTracks = List.copyOf(tracks);
    cachedMembership = Set.copyOf(tracks);
    cachedOrderChanged = !original.equals(tracks);
  }
  /** Append a saved source in its own order, retaining missing IDs. */
  public void appendTracks(Collection<String> source) {
    requireEditable();
    LinkedHashSet<String> merged = new LinkedHashSet<>(tracks);
    merged.addAll(source);
    List<String> next = List.copyOf(merged);
    MusicPlaylist.validateTracks(next);
    if (!tracks.equals(next)) { tracks.clear(); tracks.addAll(next); cachedTracks = null; }
  }
  public void include(String id, boolean value) {
    requireEditable();
    if (value) { if (!included(id)) { tracks.add(id); cachedTracks = null; } }
    else if (tracks.remove(id)) cachedTracks = null;
  }
  public void includeAll() { appendTracks(available); }
  public void excludeAll(Collection<String> ids) {
    requireEditable();
    if (tracks.removeAll(ids instanceof Set<?> ? ids : new HashSet<>(ids))) cachedTracks = null;
  }
  public void move(String id, int offset) {
    requireEditable();
    int index = tracks.indexOf(id), next = index + offset;
    if (index >= 0 && next >= 0 && next < tracks.size()) { Collections.swap(tracks, index, next); cachedTracks = null; }
  }
  public void order(List<String> order) {
    requireEditable();
    MusicPlaylist.validateTracks(order);
    if (!new HashSet<>(order).equals(new HashSet<>(tracks))) throw new IllegalArgumentException("Playlist membership changed");
    if (!tracks.equals(order)) { tracks.clear(); tracks.addAll(order); cachedTracks = null; }
  }
  public boolean changed(String name) {
    if (readOnly) return false;
    refreshSnapshot();
    return creating || !originalName.equals(favorites() ? originalName : name.strip()) || cachedOrderChanged;
  }
  public MusicPlaylist snapshot(String name) {
    return new MusicPlaylist(id, readOnly || favorites() ? originalName : name, tracks);
  }

  private void requireEditable() {
    if (readOnly) throw new IllegalStateException("This playlist is read-only");
  }
}
