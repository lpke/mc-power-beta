package local.luke.power.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** A bounded, read-only scan. Symlinks are not followed and malformed tracks are skipped. */
public final class MusicLibrary {
  public record Track(Path path, String name, Path playbackPath, String id) {
    public Track(Path path, String name, Path playbackPath) {
      this(path, name, playbackPath, "music:custom/" + Mp3Converter.hash(path.toAbsolutePath().normalize().toString()));
    }
    public Track(Path path, String name) { this(path, name, path); }
    public boolean mp3() { return name.toLowerCase(Locale.ROOT).endsWith(".mp3"); }
    public boolean playable() { return playbackPath != null; }
    public String playbackName() { return mp3() ? name.substring(0, name.length() - 4) + ".wav" : name; }
  }

  public record Scan(List<Track> tracks, List<String> warnings) {
    public Scan {
      tracks = List.copyOf(tracks);
      warnings = List.copyOf(warnings);
    }
  }

  public static final int MAX_TRACKS = 2048, MAX_VISITED = 20000;

  public static Scan scan(Path game, List<String> folders, boolean recursive) {
    Map<Path, Track> tracks = new TreeMap<>();
    List<String> warnings = new ArrayList<>();
    int visited = 0;
    for (String folder : folders) {
      Path dir;
      try {
        dir = resolve(game, folder);
        if (!Files.isDirectory(dir) || Files.isSymbolicLink(dir)) {
          warnings.add("Folder unavailable: " + folder);
          continue;
        }
      } catch (RuntimeException e) {
        warnings.add("Invalid folder: " + folder);
        continue;
      }
      try (var paths = Files.walk(dir, recursive ? 12 : 1)) {
        Iterator<Path> it = paths.iterator();
        while (it.hasNext()) {
          Path file = it.next();
          if (++visited > MAX_VISITED) {
            warnings.add("Music scan reached the 20,000 file limit");
            return new Scan(new ArrayList<>(tracks.values()), warnings);
          }
          if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
          String name = file.getFileName().toString();
          String lower = name.toLowerCase(Locale.ROOT);
          if (!lower.endsWith(".ogg") && !lower.endsWith(".wav") && !lower.endsWith(".mus") && !lower.endsWith(".mp3"))
            continue;
          if (tracks.size() >= MAX_TRACKS) {
            warnings.add("Music library reached the 2,048 track limit");
            return new Scan(new ArrayList<>(tracks.values()), warnings);
          }
          if (Files.size(file) < 4 || Files.size(file) > 256L * 1024 * 1024) {
            warnings.add("Skipped empty or oversized track: " + name);
            continue;
          }
          if (!validHeader(file, lower)) {
            warnings.add("Skipped invalid audio: " + name);
            continue;
          }
          Path canonical = file.toRealPath();
          tracks.putIfAbsent(canonical, new Track(canonical, name, lower.endsWith(".mp3") ? Mp3Converter.cached(game, canonical) : canonical));
        }
      } catch (IOException | UncheckedIOException | SecurityException e) {
        warnings.add("Could not finish reading: " + folder);
      }
    }
    return new Scan(new ArrayList<>(tracks.values()), warnings);
  }

  public static Path resolve(Path game, String path) {
    if (path.equals("~")) return Path.of(System.getProperty("user.home"));
    if (path.startsWith("~/"))
      return Path.of(System.getProperty("user.home")).resolve(path.substring(2)).normalize();
    Path p = Path.of(path);
    return (p.isAbsolute() ? p : game.resolve(p)).normalize();
  }

  static boolean audioFile(Path file) throws IOException {
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) return false;
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    if (!name.endsWith(".ogg") && !name.endsWith(".wav") && !name.endsWith(".mus") && !name.endsWith(".mp3")) return false;
    long bytes = Files.size(file);
    return bytes >= 4 && bytes <= 256L * 1024 * 1024 && validHeader(file, name);
  }

  private static boolean validHeader(Path file, String name) throws IOException {
    if (name.endsWith(".mus")) return true;
    try (InputStream in = Files.newInputStream(file)) {
      byte[] h = in.readNBytes(12);
      if (h.length < 12) return false;
      if (name.endsWith(".mp3")) return h[0] == 'I' && h[1] == 'D' && h[2] == '3' || (h[0] & 255) == 255 && (h[1] & 224) == 224;
      if (name.endsWith(".ogg")) return h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S';
      return h[0] == 'R'
          && h[1] == 'I'
          && h[2] == 'F'
          && h[3] == 'F'
          && h[8] == 'W'
          && h[9] == 'A'
          && h[10] == 'V'
          && h[11] == 'E';
    }
  }
}
