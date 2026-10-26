package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicGroups;
import local.luke.power.audio.MusicPlaylist;

/** An isolated ordered editor; unavailable entries stay in the draft until explicitly removed. */
public final class MusicPresetDraft {
  private final String id, originalName;
  private final boolean creating;
  private final List<String> original, tracks, available;
  private final Map<String, Integer> originalVolumes, volumes;

  public MusicPresetDraft(MusicPlaylist playlist) { this(playlist, List.of()); }

  public MusicPresetDraft(MusicPlaylist playlist, Collection<String> available) {
    creating = playlist == null;
    id = creating ? UUID.randomUUID().toString() : playlist.id();
    originalName = creating ? "New playlist" : playlist.name();
    original = creating ? List.of() : List.copyOf(playlist.tracks());
    tracks = new ArrayList<>(original);
    originalVolumes = creating ? Map.of() : Map.copyOf(playlist.groupVolumes());
    volumes = new TreeMap<>(originalVolumes);
    this.available = List.copyOf(new LinkedHashSet<>(available));
  }

  public String name() { return originalName; }
  public boolean creating() { return creating; }
  public boolean included(String id) { return tracks.contains(id); }
  public List<String> tracks() { return List.copyOf(tracks); }
  public void include(String id, boolean value) {
    if (value) { if (!tracks.contains(id)) tracks.add(id); }
    else tracks.remove(id);
  }
  public void includeAll() { for (String id : available) include(id, true); }
  public void excludeAll(Collection<String> ids) { tracks.removeAll(ids); }
  public void move(String id, int offset) {
    int index = tracks.indexOf(id), next = index + offset;
    if (index >= 0 && next >= 0 && next < tracks.size()) Collections.swap(tracks, index, next);
  }
  public void order(List<String> order) {
    MusicPlaylist.validateTracks(order);
    if (!new HashSet<>(order).equals(new HashSet<>(tracks))) throw new IllegalArgumentException("Playlist membership changed");
    tracks.clear(); tracks.addAll(order);
  }
  public int volume(String group) { return volumes.getOrDefault(group, 100); }
  public void volume(String group, int volume) {
    MusicGroups.copy(Map.of(group, volume));
    if (volume == 100) volumes.remove(group); else volumes.put(group, volume);
  }
  public boolean changed(String name) {
    return creating || !originalName.equals(name.strip()) || !original.equals(tracks)
        || !originalVolumes.equals(volumes);
  }
  public MusicPlaylist snapshot(String name) { return new MusicPlaylist(id, name, tracks, volumes); }
}
