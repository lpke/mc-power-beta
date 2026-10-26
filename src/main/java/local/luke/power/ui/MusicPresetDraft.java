package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicGroups;
import local.luke.power.audio.MusicPlaylist;

/** An isolated ordered editor; unavailable entries stay in the draft until explicitly removed. */
public final class MusicPresetDraft {
  private final String id, originalName;
  private final boolean creating, readOnly;
  private final List<String> original, tracks, available;
  private final Map<String, Integer> originalVolumes, volumes;
  private List<String> cachedTracks;

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
    originalVolumes = playlist == null ? Map.of() : Map.copyOf(playlist.groupVolumes());
    volumes = new TreeMap<>(originalVolumes);
    this.available = List.copyOf(new LinkedHashSet<>(available));
  }

  public static MusicPresetDraft readOnly(MusicPlaylist playlist) {
    return new MusicPresetDraft(Objects.requireNonNull(playlist), List.of(), false, true);
  }

  public String id() { return id; }
  public String name() { return originalName; }
  public boolean readOnly() { return readOnly; }
  public boolean favorites() { return MusicPlaylist.FAVORITES.equals(id); }
  public MusicPlaylist original() { return new MusicPlaylist(id, originalName, original, originalVolumes); }

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
  public boolean included(String id) { return tracks.contains(id); }
  public List<String> tracks() { if (cachedTracks == null) cachedTracks = List.copyOf(tracks); return cachedTracks; }
  /** Append a saved source in its own order, retaining missing IDs and this draft's gains. */
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
    if (value) { if (!tracks.contains(id)) { tracks.add(id); cachedTracks = null; } }
    else if (tracks.remove(id)) cachedTracks = null;
  }
  public void includeAll() { appendTracks(available); }
  public void excludeAll(Collection<String> ids) { requireEditable(); if (tracks.removeAll(ids)) cachedTracks = null; }
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
  public int volume(String group) { return volumes.getOrDefault(group, 100); }
  public void volume(String group, int volume) {
    requireEditable();
    MusicGroups.copy(Map.of(group, volume));
    if (volume == 100) volumes.remove(group); else volumes.put(group, volume);
  }
  public boolean changed(String name) {
    if (readOnly) return false;
    return creating || !originalName.equals(favorites() ? originalName : name.strip()) || !original.equals(tracks)
        || !originalVolumes.equals(volumes);
  }
  public MusicPlaylist snapshot(String name) {
    return new MusicPlaylist(id, readOnly || favorites() ? originalName : name, tracks, volumes);
  }

  private void requireEditable() {
    if (readOnly) throw new IllegalStateException("This playlist is read-only");
  }
}
