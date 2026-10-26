package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;

/** Library changes share Options' transaction; saved playlists and editor drafts remain separate. */
final class MusicEdits {
  private final PowerOptionsScreen parent;
  private JsonElement cachedFavourites, cachedPlaylists;
  private Setting groupVolumeSetting;
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
      playlistValues = List.of(Catalog.JSON.fromJson(value, MusicPlaylist[].class));
      cachedPlaylists = value;
    }
    return playlistValues;
  }
  String selected() { return setting("playlist").value.getAsString(); }
  String name(String id) {
    if (MusicPlaylist.SAME_AS_WORLD.equals(id)) return "Same as world";
    if (id.isEmpty() || MusicPlaylist.DEFAULT.equals(id)) return "Default rotation";
    return presets().stream().filter(p -> p.id().equals(id)).map(MusicPlaylist::name)
        .findFirst().orElse("Current selection");
  }
  List<String> currentTracks() {
    return new ArrayList<>(Arrays.asList(Catalog.JSON.fromJson(setting("playlistTracks").value, String[].class)));
  }
  boolean modified() {
    if (selected().isEmpty()) return false;
    return presets().stream().filter(p -> p.id().equals(selected())).findFirst()
        .map(p -> !p.tracks().equals(currentTracks())
            || !Catalog.JSON.toJsonTree(p.groupVolumes()).equals(setting("groupVolumes").value)).orElse(true);
  }
  String currentName() { return name(selected()) + (modified() ? " (modified)" : ""); }
  MusicPlaylist current(boolean copy) {
    Set<String> enabled = new HashSet<>(parent.session().settings().stream()
        .filter(s -> s.id.startsWith("audio.trackEnabled.") && s.value.getAsBoolean())
        .map(s -> s.id.substring("audio.trackEnabled.".length())).toList());
    List<String> tracks = selected().isEmpty()
        ? AudioController.worldMusicSelection(parent.client()).stream().filter(enabled::contains).toList()
        : currentTracks();
    Map<String,Integer> gains = Catalog.JSON.fromJson(setting("groupVolumes").value,
        new com.google.gson.reflect.TypeToken<TreeMap<String,Integer>>() {}.getType());
    return new MusicPlaylist(copy || selected().isEmpty() ? UUID.randomUUID().toString() : selected(),
        copy || selected().isEmpty() ? "Current selection" : name(selected()), tracks, gains);
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
    Setting s = setting("favourites"); s.value = Catalog.JSON.toJsonTree(ids); parent.changed(s);
  }
  void save(MusicPlaylist playlist) {
    List<MusicPlaylist> values = new ArrayList<>(presets());
    if (values.stream().anyMatch(p -> !p.id().equals(playlist.id()) && p.name().equalsIgnoreCase(playlist.name())))
      throw new IllegalArgumentException("A playlist with this name already exists");
    int index = -1;
    for (int i=0; i<values.size(); i++) if (values.get(i).id().equals(playlist.id())) index=i;
    if (index<0) values.add(playlist); else values.set(index, playlist);
    MusicPlaylist.validate(values, selected(), setting("menuPlaylist").value.getAsString());
    Setting s=setting("playlists"); s.value=Catalog.JSON.toJsonTree(values); s.validate(s.value); parent.changed(s);
  }
  void delete(String id) {
    List<MusicPlaylist> values = new ArrayList<>(presets()); values.removeIf(p -> p.id().equals(id));
    List<Setting> changes = new ArrayList<>();
    update(changes,"playlists",Catalog.JSON.toJsonTree(values));
    if (selected().equals(id)) { update(changes,"playlist",new JsonPrimitive("")); update(changes,"playlistTracks",new JsonArray()); }
    if (setting("menuPlaylist").value.getAsString().equals(id)) {
      update(changes,"menuPlaylist",new JsonPrimitive(MusicPlaylist.SAME_AS_WORLD));
      update(changes,"menuPlaylistTracks",new JsonArray()); update(changes,"menuGroupVolumes",new JsonObject());
    }
    syncRows(); parent.changed(changes,false);
  }
  private void update(List<Setting> changes,String id,JsonElement value) {
    Setting s=setting(id); s.value=value; changes.add(s);
  }
  void load(String id) {
    MusicPlaylist playlist = presets().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    if (!id.isEmpty() && playlist==null) throw new IllegalArgumentException("Playlist no longer exists");
    List<Setting> changes=new ArrayList<>();
    update(changes,"playlist",new JsonPrimitive(id));
    update(changes,"playlistTracks",playlist==null ? new JsonArray() : Catalog.JSON.toJsonTree(playlist.tracks()));
    if (playlist!=null) update(changes,"groupVolumes",Catalog.JSON.toJsonTree(playlist.groupVolumes()));
    syncRows(); parent.changed(changes,false);
  }
  void loadMenu(String id) {
    MusicPlaylist playlist = presets().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    if (playlist==null && !MusicPlaylist.SAME_AS_WORLD.equals(id) && !MusicPlaylist.DEFAULT.equals(id))
      throw new IllegalArgumentException("Playlist no longer exists");
    List<Setting> changes=new ArrayList<>();
    update(changes,"menuPlaylist",new JsonPrimitive(id));
    update(changes,"menuPlaylistTracks",playlist==null ? new JsonArray() : Catalog.JSON.toJsonTree(playlist.tracks()));
    update(changes,"menuGroupVolumes",playlist==null ? new JsonObject() : Catalog.JSON.toJsonTree(playlist.groupVolumes()));
    parent.changed(changes,false);
  }
  private void syncRows() {
    Set<String> included=new HashSet<>(currentTracks()), excluded=new HashSet<>();
    setting("exclusions").value.getAsJsonArray().forEach(v -> excluded.add(v.getAsString()));
    for (Setting s:parent.session().settings()) if (s.id.startsWith("audio.trackEnabled.")) {
      String id=s.id.substring("audio.trackEnabled.".length());
      s.value=new JsonPrimitive(selected().isEmpty() ? !excluded.contains(id) : included.contains(id));
    }
  }
  private Setting groupVolumes() {
    if (groupVolumeSetting==null) groupVolumeSetting=setting("groupVolumes"); return groupVolumeSetting;
  }
  int volume(String group) {
    JsonElement v=groupVolumes().value.getAsJsonObject().get(group); return v==null ? 100 : v.getAsInt();
  }
  void volume(String group,int volume) {
    MusicGroups.copy(Map.of(group,volume)); if (volume(group)==volume) return;
    Setting s=groupVolumes(); JsonObject values=s.value.getAsJsonObject().deepCopy();
    if (volume==100) values.remove(group); else values.addProperty(group,volume);
    s.validate(values); s.value=values; parent.changed(s,true);
  }
  void includeAll() { toggleAll(true, parent.session().settings().stream()
      .filter(s -> s.id.startsWith("audio.trackEnabled.")).map(s -> s.id.substring("audio.trackEnabled.".length())).toList()); }
  void excludeAll(Collection<String> tracks) { toggleAll(false,tracks); }
  private void toggleAll(boolean include,Collection<String> ids) {
    List<Setting> changes=new ArrayList<>();
    for (Setting s:parent.session().settings()) if (s.id.startsWith("audio.trackEnabled.")
        && ids.contains(s.id.substring("audio.trackEnabled.".length()))) {
      s.value=new JsonPrimitive(include); parent.session().link(s); changes.add(s);
    }
    // Membership is already linked. Do not replay synthetic rows into a second membership edit.
    changes.removeIf(s -> s.id.startsWith("audio.trackEnabled."));
    parent.changed(changes,false);
  }
}
