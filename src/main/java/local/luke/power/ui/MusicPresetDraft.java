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

  public MusicPresetDraft(MusicPreset preset) {
    creating = preset == null;
    id = creating ? UUID.randomUUID().toString() : preset.id();
    originalName = creating ? "New preset" : preset.name();
    original = creating ? Set.of() : Set.copyOf(preset.excluded());
    excluded = new TreeSet<>(original);
    originalVolumes = creating ? Map.of() : Map.copyOf(preset.groupVolumes());
    volumes = new TreeMap<>(originalVolumes);
  }

  public String name() {
    return originalName;
  }

  public boolean creating() {
    return creating;
  }

  public boolean included(String id) {
    return !excluded.contains(id);
  }

  public void include(String id, boolean value) {
    if (value) excluded.remove(id);
    else excluded.add(id);
  }

  public void includeAll() {
    excluded.clear();
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
        || !originalVolumes.equals(volumes);
  }

  public MusicPreset snapshot(String name) {
    return new MusicPreset(id, name, excluded, volumes);
  }
}
