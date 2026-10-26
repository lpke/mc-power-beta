package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;

/** Saved playlists and library preferences share the surrounding Options transaction. */
final class MusicEdits {
  private final PowerOptionsScreen parent;
  private JsonElement cachedFavourites, cachedPlaylists, cachedFavoriteVolumes;
  private Set<String> favouriteIds = Set.of();
  private List<MusicPlaylist> playlistValues = List.of();

  MusicEdits(PowerOptionsScreen parent) { this.parent = parent; }
  Setting setting(String id) {
    return parent.session().settings().stream().filter(s -> s.id.equals("audio." + id)).findFirst().orElseThrow();
  }
  List<MusicPlaylist> presets() {
    JsonElement value = setting("playlists").value, favorites = setting("favourites").value,
        volumes = setting("favoritesGroupVolumes").value;
    if (value != cachedPlaylists || favorites != cachedFavourites || volumes != cachedFavoriteVolumes) {
      List<MusicPlaylist> values = new ArrayList<>();
      values.add(MusicPlaylist.beta());
      var gains = new TreeMap<String, Integer>();
      volumes.getAsJsonObject().entrySet().forEach(e -> gains.put(e.getKey(), e.getValue().getAsInt()));
      values.add(new MusicPlaylist(MusicPlaylist.FAVORITES, "Favorites",
          Arrays.asList(Catalog.JSON.fromJson(favorites, String[].class)), gains));
      values.addAll(Arrays.asList(Catalog.JSON.fromJson(value, MusicPlaylist[].class)));
      playlistValues = List.copyOf(values);
      favouriteIds = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(Catalog.JSON.fromJson(favorites, String[].class))));
      cachedPlaylists = value; cachedFavourites = favorites; cachedFavoriteVolumes = volumes;
    }
    return playlistValues;
  }
  String selected() { return setting("playlist").value.getAsString(); }
  String name(String id) {
    if (MusicPlaylist.SAME_AS_WORLD.equals(id)) return "Same as world";
    return presets().stream().filter(p -> p.id().equals(id)).map(MusicPlaylist::name).findFirst().orElse("Unavailable playlist");
  }
  String currentName() { return name(selected()); }
  Set<String> favourites() { presets(); return favouriteIds; }
  void favourite(String id) {
    Set<String> ids = new LinkedHashSet<>(favourites());
    if (!ids.remove(id)) ids.add(id);
    apply(Map.of("favourites", Catalog.JSON.toJsonTree(ids)));
  }
  void libraryOrder(List<String> groups) {
    MusicLibrarySources.validateOrder(groups);
    apply(Map.of("libraryOrder", Catalog.JSON.toJsonTree(groups)));
  }
  void save(MusicPlaylist playlist) { save(playlist, null); }
  void save(MusicPlaylist playlist, MusicPlaylist expected) {
    if (MusicPlaylist.BETA.equals(playlist.id())) throw new IllegalArgumentException("Copy Beta to make your own playlist");
    List<MusicPlaylist> values = new ArrayList<>(presets());
    MusicPlaylist current = values.stream().filter(p -> p.id().equals(playlist.id())).findFirst().orElse(null);
    if (expected != null && (current == null || !Catalog.JSON.toJson(current).equals(Catalog.JSON.toJson(expected))))
      throw new IllegalArgumentException("This playlist changed. Reopen it before saving.");
    if (MusicPlaylist.FAVORITES.equals(playlist.id())) {
      if (!playlist.name().equals("Favorites")) throw new IllegalArgumentException("Favorites cannot be renamed");
      apply(Map.of("favourites", Catalog.JSON.toJsonTree(playlist.tracks()),
          "favoritesGroupVolumes", Catalog.JSON.toJsonTree(playlist.groupVolumes())));
      return;
    }
    if (values.stream().anyMatch(p -> !p.id().equals(playlist.id()) && p.name().equalsIgnoreCase(playlist.name())))
      throw new IllegalArgumentException("A playlist with this name already exists");
    values.removeIf(p -> MusicPlaylist.BETA.equals(p.id()) || MusicPlaylist.FAVORITES.equals(p.id()));
    int index = -1;
    for (int i = 0; i < values.size(); i++) if (values.get(i).id().equals(playlist.id())) index = i;
    if (index < 0) values.add(playlist); else values.set(index, playlist);
    MusicPlaylist.validate(values, selected(), setting("menuPlaylist").value.getAsString());
    apply(Map.of("playlists", Catalog.JSON.toJsonTree(values)));
  }
  MusicPlaylist copy(MusicPlaylist playlist) {
    String name = MusicPresetDraft.copyName(playlist.name(), presets().stream().map(MusicPlaylist::name).toList());
    return new MusicPlaylist(UUID.randomUUID().toString(), name, playlist.tracks(), playlist.groupVolumes());
  }
  void delete(String id) {
    if (MusicPlaylist.BETA.equals(id) || MusicPlaylist.FAVORITES.equals(id)) return;
    List<MusicPlaylist> values = new ArrayList<>(presets());
    values.removeIf(p -> p.id().equals(id) || MusicPlaylist.BETA.equals(p.id()) || MusicPlaylist.FAVORITES.equals(p.id()));
    Map<String, JsonElement> changes = new LinkedHashMap<>();
    changes.put("playlists", Catalog.JSON.toJsonTree(values));
    if (selected().equals(id)) changes.put("playlist", new JsonPrimitive(MusicPlaylist.BETA));
    if (setting("menuPlaylist").value.getAsString().equals(id)) changes.put("menuPlaylist", new JsonPrimitive(MusicPlaylist.SAME_AS_WORLD));
    apply(changes);
  }
  private void apply(Map<String, JsonElement> values) {
    var draft = new SettingsDraft(values.keySet().stream().map(this::setting).toList(), ignored -> true);
    values.forEach((id, value) -> { setting(id).validate(value); draft.get("audio." + id).value = value.deepCopy(); });
    if (!parent.acceptEditor(draft)) throw new IllegalArgumentException(parent.editorError());
  }
  void load(String id) {
    if (presets().stream().noneMatch(p -> p.id().equals(id))) throw new IllegalArgumentException("Playlist no longer exists");
    apply(Map.of("playlist", new JsonPrimitive(id)));
  }
  void loadMenu(String id) {
    if (!MusicPlaylist.SAME_AS_WORLD.equals(id) && presets().stream().noneMatch(p -> p.id().equals(id)))
      throw new IllegalArgumentException("Playlist no longer exists");
    apply(Map.of("menuPlaylist", new JsonPrimitive(id)));
  }
}
