package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicPreset;

/** An editor owns its own sets, including missing tracks; it never mutates the live selection. */
public final class MusicPresetDraft {
  private final String id, originalName;
  private final Set<String> original;
  private final boolean creating;
  private final Set<String> excluded;

  public MusicPresetDraft(MusicPreset preset) {
    creating = preset == null;
    id = creating ? UUID.randomUUID().toString() : preset.id();
    originalName = creating ? "New preset" : preset.name();
    original = creating ? Set.of() : Set.copyOf(preset.excluded());
    excluded = new TreeSet<>(original);
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

  public boolean changed(String name) {
    return creating || !originalName.equals(name.strip()) || !original.equals(excluded);
  }

  public MusicPreset snapshot(String name) {
    return new MusicPreset(id, name, excluded);
  }
}
