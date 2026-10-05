package local.luke.power.audio;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pinned game asset metadata; filenames remain stable setting and queue identifiers. */
public final class BuiltinMusic {
  private BuiltinMusic() {}
  public record Track(String file, String title, String era, String usage, String sha1, int size) {
    public String id() { return "music:" + file; }
    public boolean creative() { return usage.equals("Creative"); }
    public boolean background() { return usage.equals("Overworld") || creative(); }
    public String group() { return era + (usage.equals("Overworld") ? "" : " / " + usage); }
    public boolean included(AudioSettings.MusicMode mode) {
      if (usage.equals("Nether") || usage.equals("End")) return false;
      boolean early = era.equals("Alpha") || era.equals("Beta");
      return switch (mode) {
        case VANILLA -> era.equals("Alpha") && usage.equals("Overworld");
        case ALPHA_BETA_SURVIVAL -> early && usage.equals("Overworld");
        case ALPHA_BETA -> early;
        case MINECRAFT_SURVIVAL -> usage.equals("Overworld");
        case ALL_MINECRAFT -> true;
      };
    }
  }
  public static final List<Track> TRACKS = load();
  /** Manifest order is chronological; shared survival/creative tracks appear only once. */
  public static final List<String> GROUPS = groups();
  private static List<String> groups() {
    List<String> eras = List.of("Alpha", "Beta", "Update Aquatic", "Nether Update", "Caves & Cliffs",
        "The Wild Update", "Trails & Tales", "Tricky Trials", "Chase the Skies", "Chaos Cubed");
    List<String> uses = List.of("Overworld", "Creative", "Menu", "Records", "Credits", "Album extras", "Nether", "End");
    return TRACKS.stream().sorted(Comparator.comparingInt((Track t) -> eras.indexOf(t.era()))
        .thenComparingInt(t -> uses.indexOf(t.usage()))).map(Track::group).distinct().toList();
  }
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
            t.get("era").getAsString(), t.get("usage").getAsString(),
            t.get("sha1").getAsString(), t.get("size").getAsInt());
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
