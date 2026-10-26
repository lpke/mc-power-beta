package local.luke.power.validation;

import java.nio.file.Path;
import java.util.*;
import local.luke.power.audio.*;

/** Fixture-only saved-playlist mutations. Production has no working selection. */
final class MusicFixtureSupport {
  private MusicFixtureSupport() {}

  static List<String> tracks(AudioSettings settings) {
    return MusicPlaylist.find(settings, settings.playlist).tracks();
  }

  static List<String> menuTracks(AudioSettings settings) {
    return MusicPlaylist.find(settings, settings.menuPlaylist.equals(MusicPlaylist.SAME_AS_WORLD)
        ? settings.playlist : settings.menuPlaylist).tracks();
  }

  static void selection(AudioSettings settings, Collection<String> tracks) {
    replace(settings, List.copyOf(new LinkedHashSet<>(tracks)), volumes(settings));
  }

  static void allBuiltin(AudioSettings settings) {
    selection(settings, BuiltinMusic.TRACKS.stream().map(BuiltinMusic.Track::id).toList());
  }

  static void imported(AudioSettings settings, boolean includeBeta) {
    var tracks = new ArrayList<String>();
    if (includeBeta) tracks.addAll(BuiltinMusic.VANILLA);
    var folders = new ArrayList<>(settings.musicDirectories);
    for (var track : MusicLibrary.scan(Path.of(".").toAbsolutePath(), folders).tracks())
      tracks.add(track.id());
    selection(settings, tracks);
  }

  private static void replace(AudioSettings settings, List<String> tracks, Map<String, Integer> gains) {
    String id = settings.playlist.equals(MusicPlaylist.BETA) ? "native-fixture" : settings.playlist;
    String name = settings.playlist.equals(MusicPlaylist.BETA) ? "Native fixture"
        : MusicPlaylist.find(settings, settings.playlist).name();
    var playlist = new MusicPlaylist(id, name, tracks, gains);
    for (int i = 0; i < settings.playlists.size(); i++)
      if (settings.playlists.get(i).id().equals(id)) { settings.playlists.set(i, playlist); settings.playlist = id; return; }
    settings.playlists.add(playlist); settings.playlist = id;
  }

  static void volumes(AudioSettings settings, Map<String, Integer> gains) {
    replace(settings, tracks(settings), new TreeMap<>(gains));
  }

  static Map<String, Integer> volumes(AudioSettings settings) {
    return MusicPlaylist.find(settings, settings.playlist).groupVolumes();
  }

  static void volume(AudioSettings settings, String group, int gain) {
    var copy = new TreeMap<>(volumes(settings));copy.put(group,gain);volumes(settings,copy);
  }

  static void omit(AudioSettings settings, String id) {
    var copy = new ArrayList<>(tracks(settings));copy.remove(id);selection(settings,copy);
  }
}
