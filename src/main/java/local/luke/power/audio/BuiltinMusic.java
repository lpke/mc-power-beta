package local.luke.power.audio;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pinned game asset metadata; filenames remain stable setting and queue identifiers. */
public final class BuiltinMusic {
  private BuiltinMusic() {}
  public record Track(String file, String title, String era, String sha1, int size) {
    public String id() { return "music:" + file; }
    public boolean included(AudioSettings.MusicMode mode) {
      return era.equals("Alpha") || mode == AudioSettings.MusicMode.ALL_MINECRAFT
          || mode == AudioSettings.MusicMode.ALPHA_BETA && era.equals("Beta");
    }
  }
  public static final List<Track> TRACKS = load();
  private static List<Track> load() {
    try (var in = BuiltinMusic.class.getResourceAsStream("/assets/powerbeta/music/manifest.json")) {
      if (in == null) throw new IOException("Missing soundtrack manifest");
      JsonArray values = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonArray("tracks");
      List<Track> tracks = new ArrayList<>();
      Set<String> names = new HashSet<>();
      for (JsonElement value : values) {
        JsonObject t = value.getAsJsonObject();
        Track track = new Track(t.get("file").getAsString(), t.get("title").getAsString(),
            t.get("era").getAsString(), t.get("sha1").getAsString(), t.get("size").getAsInt());
        if (!track.file.matches("[a-z0-9_]+\\.ogg") || !names.add(track.file))
          throw new IOException("Invalid soundtrack manifest");
        tracks.add(track);
      }
      return List.copyOf(tracks);
    } catch (IOException | RuntimeException e) { throw new ExceptionInInitializerError(e); }
  }

  public static Track find(String id) {
    String stem = id.replaceFirst("^music:", "").replaceFirst("\\.[^.]+$", "");
    return TRACKS.stream().filter(t -> t.file.equals(stem + ".ogg")).findFirst().orElse(null);
  }

  public static String label(String filename) {
    Track track = find(filename);
    String stem = filename.replaceFirst("\\.[^.]+$", "").replace('_', ' ');
    return track == null || stem.equalsIgnoreCase(track.title) ? filename : filename + " (" + track.title + ")";
  }
}
