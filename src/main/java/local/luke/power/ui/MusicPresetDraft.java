package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicGroups;
import local.luke.power.audio.MusicPreset;

/** An editor owns its own sets, including missing tracks; it never mutates the live selection. */
public final class MusicPresetDraft {
  private final String id, originalName;
  private final Set<String> original;
  private final boolean creating;
  private final Set<String> excluded;
  private final Map<String, Integer> originalVolumes, volumes;
  private final Set<String> available, originalPool, pool;

  public MusicPresetDraft(MusicPreset preset) {
    this(preset, Set.of());
  }

  public MusicPresetDraft(MusicPreset preset, Collection<String> available) {
    creating = preset == null;
    id = creating ? UUID.randomUUID().toString() : preset.id();
    originalName = creating ? "New preset" : preset.name();
    original = creating ? Set.of() : Set.copyOf(preset.excluded());
    excluded = new TreeSet<>(original);
    originalVolumes = creating ? Map.of() : Map.copyOf(preset.groupVolumes());
    volumes = new TreeMap<>(originalVolumes);
    this.available = Set.copyOf(available);
    originalPool = creating ? Set.of() : Set.copyOf(preset.trackPool());
    pool = new TreeSet<>(creating ? available : originalPool);
  }

  public String name() {
    return originalName;
  }

  public boolean creating() {
    return creating;
  }

  public boolean included(String id) {
    return pool.contains(id) && !excluded.contains(id);
  }

  public void include(String id, boolean value) {
    if (value) { excluded.remove(id); pool.add(id); }
    else excluded.add(id);
  }

  public void includeAll() {
    excluded.clear();
    pool.addAll(available);
  }

  public void excludeAll(Collection<String> tracks) {
    Set<String> next = new TreeSet<>(excluded);
    next.addAll(tracks);
    MusicPreset.validateTracks(next);
    excluded.clear();
    excluded.addAll(next);
  }

  public int volume(String group) {
    return volumes.getOrDefault(group, 100);
  }

  public void volume(String group, int volume) {
    MusicGroups.copy(Map.of(group, volume));
    if (volume == 100) volumes.remove(group);
    else volumes.put(group, volume);
  }

  public boolean changed(String name) {
    return creating
        || !originalName.equals(name.strip())
        || !original.equals(excluded)
        || !originalVolumes.equals(volumes)
        || !originalPool.equals(pool);
  }

  public MusicPreset snapshot(String name) {
    return new MusicPreset(id, name, excluded, volumes,
        pool);
  }
}
