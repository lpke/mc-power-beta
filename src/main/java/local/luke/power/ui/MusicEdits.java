package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;

/** Saved playlists and library preferences share the surrounding Options transaction. */
final class MusicEdits {
  private final PowerOptionsScreen parent;
  private JsonElement cachedFavourites, cachedPlaylists;
  private Set<String> favouriteIds = Set.of();
  private List<MusicPlaylist> playlistValues = List.of();

  MusicEdits(PowerOptionsScreen parent) { this.parent = parent; }

  Setting setting(String id) {
    return parent.session().settings().stream().filter(s -> s.id.equals("audio." + id))
        .findFirst().orElseThrow();
  }

  List<MusicPlaylist> presets() {
    JsonElement value = setting("playlists").value;
    if (value != cachedPlaylists) {
      List<MusicPlaylist> values = new ArrayList<>();
      values.add(MusicPlaylist.beta());
      values.addAll(Arrays.asList(Catalog.JSON.fromJson(value, MusicPlaylist[].class)));
      playlistValues = List.copyOf(values);
      cachedPlaylists = value;
    }
    return playlistValues;
  }

  String selected() { return setting("playlist").value.getAsString(); }

  String name(String id) {
    if (MusicPlaylist.SAME_AS_WORLD.equals(id)) return "Same as world";
    return presets().stream().filter(p -> p.id().equals(id)).map(MusicPlaylist::name)
        .findFirst().orElse("Unavailable playlist");
  }

  String currentName() { return name(selected()); }

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

  void save(MusicPlaylist playlist) {
    if (MusicPlaylist.BETA.equals(playlist.id()))
      throw new IllegalArgumentException("Copy Beta to make your own playlist");
    List<MusicPlaylist> values = new ArrayList<>(presets());
    if (values.stream().anyMatch(p -> !p.id().equals(playlist.id()) && p.name().equalsIgnoreCase(playlist.name())))
      throw new IllegalArgumentException("A playlist with this name already exists");
    values.removeIf(p -> MusicPlaylist.BETA.equals(p.id()));
    int index = -1;
    for (int i = 0; i < values.size(); i++) if (values.get(i).id().equals(playlist.id())) index = i;
    if (index < 0) values.add(playlist); else values.set(index, playlist);
    MusicPlaylist.validate(values, selected(), setting("menuPlaylist").value.getAsString());
    Setting s = setting("playlists");
    s.value = Catalog.JSON.toJsonTree(values);
    s.validate(s.value);
    parent.changed(s);
  }

  MusicPlaylist copy(MusicPlaylist playlist) {
    String name = MusicPresetDraft.copyName(playlist.name(), presets().stream().map(MusicPlaylist::name).toList());
    return new MusicPlaylist(UUID.randomUUID().toString(), name, playlist.tracks(), playlist.groupVolumes());
  }

  void delete(String id) {
    if (MusicPlaylist.BETA.equals(id)) return;
    List<MusicPlaylist> values = new ArrayList<>(presets());
    values.removeIf(p -> p.id().equals(id) || MusicPlaylist.BETA.equals(p.id()));
    List<Setting> changes = new ArrayList<>();
    update(changes, "playlists", Catalog.JSON.toJsonTree(values));
    if (selected().equals(id)) update(changes, "playlist", new JsonPrimitive(MusicPlaylist.BETA));
    if (setting("menuPlaylist").value.getAsString().equals(id))
      update(changes, "menuPlaylist", new JsonPrimitive(MusicPlaylist.SAME_AS_WORLD));
    parent.changed(changes, false);
  }

  private void update(List<Setting> changes, String id, JsonElement value) {
    Setting s = setting(id);
    s.value = value;
    changes.add(s);
  }

  void load(String id) {
    if (presets().stream().noneMatch(p -> p.id().equals(id)))
      throw new IllegalArgumentException("Playlist no longer exists");
    Setting s = setting("playlist");
    s.value = new JsonPrimitive(id);
    parent.changed(s);
  }

  void loadMenu(String id) {
    if (!MusicPlaylist.SAME_AS_WORLD.equals(id) && presets().stream().noneMatch(p -> p.id().equals(id)))
      throw new IllegalArgumentException("Playlist no longer exists");
    Setting s = setting("menuPlaylist");
    s.value = new JsonPrimitive(id);
    parent.changed(s);
  }
}
