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

  /** Filesystem observations collected only on the scanner thread. */
  public record SourceFolder(String path, Path root, boolean available, List<String> warnings) {
    public SourceFolder { warnings = List.copyOf(warnings); }
  }

  public record Scan(List<Track> tracks, List<String> warnings, List<SourceFolder> sources) {
    public Scan(List<Track> tracks, List<String> warnings) { this(tracks, warnings, List.of()); }
    public Scan {
      tracks = List.copyOf(tracks);
      warnings = List.copyOf(warnings);
      sources = List.copyOf(sources);
    }
  }

  public static final int MAX_TRACKS = 2048, MAX_VISITED = 20000;

  public static Scan scan(Path game, List<String> folders, boolean recursive) {
    Map<Path, Track> tracks = new TreeMap<>();
    List<String> warnings = new ArrayList<>();
    List<SourceFolder> sources = new ArrayList<>();
    // Resolve every enabled root before walking. Even an overlapping root reached
    // after the scan budget runs out needs a canonical identity for attribution.
    for (String folder : folders) {
      Path dir = null;
      String warning = "";
      try {
        dir = resolve(game, folder).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir) || Files.isSymbolicLink(dir) || !Files.isReadable(dir))
          warning = "Folder unavailable: " + folder;
        else dir = dir.toRealPath();
      } catch (IOException | SecurityException e) {
        warning = "Folder unavailable: " + folder;
      } catch (RuntimeException e) {
        warning = "Invalid folder: " + folder;
      }
      sources.add(new SourceFolder(folder, dir, warning.isEmpty(),
          warning.isEmpty() ? List.of() : List.of(warning)));
      if (!warning.isEmpty()) warnings.add(warning);
    }
    int visited = 0;
    boolean limited = false;
    for (int index = 0; index < sources.size(); index++) {
      SourceFolder source = sources.get(index);
      if (!source.available()) continue;
      List<String> sourceWarnings = new ArrayList<>();
      if (limited) {
        sources.set(index, new SourceFolder(source.path(), source.root(), true,
            List.of("Folder scan incomplete: music scan limit reached")));
        continue;
      }
      try (var paths = Files.walk(source.root(), recursive ? 12 : 1)) {
        Iterator<Path> it = paths.iterator();
        while (it.hasNext()) {
          Path file = it.next();
          if (++visited > MAX_VISITED) {
            sourceWarnings.add("Music scan reached the 20,000 file limit");
            limited = true;
            break;
          }
          if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
          String name = file.getFileName().toString();
          String lower = name.toLowerCase(Locale.ROOT);
          if (!lower.endsWith(".ogg") && !lower.endsWith(".wav") && !lower.endsWith(".mus") && !lower.endsWith(".mp3"))
            continue;
          long bytes = Files.size(file);
          if (bytes < 4 || bytes > 256L * 1024 * 1024) {
            sourceWarnings.add("Skipped empty or oversized track: " + name);
            continue;
          }
          if (!validHeader(file, lower)) {
            sourceWarnings.add("Skipped invalid audio: " + name);
            continue;
          }
          Path canonical = file.toRealPath();
          if (tracks.containsKey(canonical)) continue;
          if (tracks.size() >= MAX_TRACKS) {
            sourceWarnings.add("Music library reached the 2,048 track limit");
            limited = true;
            break;
          }
          tracks.put(canonical, new Track(canonical, name, lower.endsWith(".mp3") ? Mp3Converter.cached(game, canonical) : canonical));
        }
      } catch (IOException | UncheckedIOException | SecurityException e) {
        sourceWarnings.add("Could not finish reading: " + source.path());
      }
      warnings.addAll(sourceWarnings);
      sources.set(index, new SourceFolder(source.path(), source.root(), true, sourceWarnings));
    }
    return new Scan(new ArrayList<>(tracks.values()), warnings, sources);
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
