package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;

/**
 * All library changes share the Options transaction. Editor drafts never reference these values.
 */
final class MusicEdits {
  private final PowerOptionsScreen parent;
  private JsonElement cachedFavourites, cachedPresets;
  private Set<String> favouriteIds = Set.of();
  private List<MusicPreset> presetValues = List.of();

  MusicEdits(PowerOptionsScreen parent) {
    this.parent = parent;
  }

  Setting setting(String id) {
    return parent.session().settings().stream()
        .filter(s -> s.id.equals("audio." + id))
        .findFirst()
        .orElseThrow();
  }

  List<MusicPreset> presets() {
    JsonElement value = setting("presets").value;
    if (value != cachedPresets) {
      presetValues = List.of(Catalog.JSON.fromJson(value, MusicPreset[].class));
      cachedPresets = value;
    }
    return presetValues;
  }

  String selected() {
    return setting("preset").value.getAsString();
  }

  String name(String id) {
    return presets().stream()
        .filter(p -> p.id().equals(id))
        .map(MusicPreset::name)
        .findFirst()
        .orElse("None");
  }

  Set<String> favourites() {
    JsonElement value = setting("favourites").value;
    if (value != cachedFavourites) {
      favouriteIds = Set.copyOf(Arrays.asList(Catalog.JSON.fromJson(value, String[].class)));
      cachedFavourites = value;
    }
    return favouriteIds;
  }

  void favourite(String id) {
    Set<String> ids = new TreeSet<>(favourites());
    if (!ids.remove(id)) ids.add(id);
    Setting s = setting("favourites");
    s.value = Catalog.JSON.toJsonTree(ids);
    parent.changed(s);
  }

  void save(MusicPreset preset) {
    List<MusicPreset> values = new ArrayList<>(presets());
    if (values.stream().anyMatch(p -> !p.id().equals(preset.id()) && p.name().equalsIgnoreCase(preset.name())))
      throw new IllegalArgumentException("A preset with this name already exists");
    int index = -1;
    for (int i = 0; i < values.size(); i++) if (values.get(i).id().equals(preset.id())) index = i;
    if (index < 0) values.add(preset); else values.set(index, preset);
    MusicPreset.validate(values, selected());
    Setting s = setting("presets");
    JsonElement value = Catalog.JSON.toJsonTree(values);
    s.validate(value);
    s.value = value;
    parent.changed(s);
  }

  void delete(String id) {
    List<MusicPreset> values = new ArrayList<>(presets());
    values.removeIf(p -> p.id().equals(id));
    Setting list = setting("presets"), selected = setting("preset");
    list.value = Catalog.JSON.toJsonTree(values);
    if (selected.value.getAsString().equals(id)) selected.value = new JsonPrimitive("");
    parent.changed(List.of(list, selected), false);
  }

  void load(String id) {
    MusicPreset p = presets().stream().filter(v -> v.id().equals(id)).findFirst().orElse(null);
    if (!id.isEmpty() && p == null) throw new IllegalArgumentException("Preset no longer exists");
    List<Setting> changes = new ArrayList<>();
    if (p != null) changes.addAll(exclusions(p.excluded()));
    Setting selected = setting("preset");
    selected.value = new JsonPrimitive(id);
    changes.add(selected);
    parent.changed(changes, false);
  }

  private List<Setting> exclusions(Set<String> excluded) {
    List<Setting> changes = new ArrayList<>();
    Setting all = setting("exclusions");
    all.value = Catalog.JSON.toJsonTree(excluded);
    changes.add(all);
    for (Setting s : parent.session().settings())
      if (s.id.startsWith("audio.trackEnabled.")) {
        s.value =
            new JsonPrimitive(!excluded.contains(s.id.substring("audio.trackEnabled.".length())));
        changes.add(s);
      }
    return changes;
  }

  void includeAll() {
    parent.changed(exclusions(Set.of()), false);
  }
}
