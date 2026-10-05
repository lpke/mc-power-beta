package local.luke.power.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Explicit, cancellable conversion into an instance-local cache. Originals are never written. */
public final class Mp3Converter {
  private static final ExecutorService WORKER =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Power Beta MP3 conversion");
            t.setDaemon(true);
            return t;
          });
  private static volatile boolean busy, cancelled;
  private static volatile Process process;
  private static volatile String status = "";
  private static volatile int revision;
  private static volatile boolean available;
  private static boolean probed;

  /** One bounded probe off the client thread; conversion never installs system software. */
  public static synchronized void probe() {
    if (probed) return;
    probed = true;
    WORKER.execute(() -> available = probeExecutable("ffmpeg"));
  }

  static boolean probeExecutable(String executable) {
    Process check = null;
    try {
      check = new ProcessBuilder(executable, "-version")
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      return check.waitFor(3, TimeUnit.SECONDS) && check.exitValue() == 0;
    } catch (IOException | SecurityException ignored) {
      return false;
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      if (check != null && check.isAlive()) check.destroyForcibly();
    }
  }

  public static boolean available() { return available; }

  public static String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Path destination(Path game, Path source) throws IOException {
    String key =
        source.toAbsolutePath().normalize()
            + ":"
            + Files.size(source)
            + ":"
            + Files.getLastModifiedTime(source).toMillis();
    return game.resolve("power-beta-data/music-cache").resolve(hash(key) + ".wav");
  }

  public static Path cached(Path game, Path source) throws IOException {
    Path file = destination(game, source);
    return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Files.size(file) > 44
        ? file
        : null;
  }

  public static boolean busy() {
    return busy;
  }

  public static boolean needed(Collection<MusicLibrary.Track> tracks) {
    return tracks.stream().anyMatch(t -> t.mp3() && !t.playable());
  }

  public static String status() {
    return status;
  }

  public static int revision() {
    return revision;
  }

  public static void cancel() {
    cancelled = true;
    Process p = process;
    if (p != null) p.destroy();
  }

  public static synchronized void start(Path game, List<MusicLibrary.Track> tracks) {
    if (busy) return;
    if (!available) { status = "FFmpeg is unavailable"; return; }
    List<Path> sources =
        tracks.stream()
            .filter(t -> t.mp3() && !t.playable())
            .map(MusicLibrary.Track::path)
            .distinct()
            .limit(256)
            .toList();
    if (sources.isEmpty()) {
      status = "No MP3 files need conversion";
      return;
    }
    busy = true;
    cancelled = false;
    status = "Starting MP3 conversion...";
    WORKER.execute(
        () -> {
          int completed = 0, failed = 0;
          String reason = "";
          try {
            for (Path source : sources) {
              if (cancelled) break;
              status =
                  "Converting "
                      + (completed + failed + 1)
                      + "/"
                      + sources.size()
                      + ": "
                      + source.getFileName();
              try {
                convert(game, source);
                completed++;
              } catch (IOException e) {
                failed++;
                reason = Objects.toString(e.getMessage(), "Conversion failed");
              }
            }
          } finally {
            process = null;
            busy = false;
            revision++;
            status =
                (cancelled ? "Cancelled. " : "")
                    + completed
                    + " converted"
                    + (failed == 0 ? "" : "; " + failed + " failed: " + reason);
          }
        });
  }

  static void convert(Path game, Path source) throws IOException {
    if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
        || Files.size(source) > 256L * 1024 * 1024)
      throw new IOException("MP3 unavailable or larger than 256 MiB");
    Path target = destination(game, source);
    Files.createDirectories(target.getParent());
    // Do not write through a user-created cache-directory symlink.
    Path expected = game.toRealPath().resolve("power-beta-data/music-cache");
    if (!target.getParent().toRealPath().equals(expected))
      throw new IOException("Music cache must be inside this instance");
    Path temp = Files.createTempFile(target.getParent(), "convert-", ".wav");
    Path log = Files.createTempFile(target.getParent(), "convert-", ".log");
    try {
      try {
        process =
            new ProcessBuilder(
                    "ffmpeg",
                    "-nostdin",
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-y",
                    "-i",
                    source.toString(),
                    "-vn",
                    "-acodec",
                    "pcm_s16le",
                    "-ac",
                    "2",
                    "-ar",
                    "44100",
                    "-f",
                    "wav",
                    temp.toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
      } catch (IOException e) {
        throw new IOException("FFmpeg unavailable. Install FFmpeg to convert MP3 files.", e);
      }
      try {
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(3);
        while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
          if (cancelled
              || System.nanoTime() > deadline
              || Files.size(temp) > 1024L * 1024 * 1024
              || Files.size(log) > 1024 * 1024) {
            process.destroyForcibly();
            throw new IOException(
                cancelled ? "Cancelled" : "Conversion exceeded its time or size limit");
          }
        }
        if (cancelled
            || process.exitValue() != 0
            || Files.size(temp) < 44
            || Files.size(temp) > 1024L * 1024 * 1024)
          throw new IOException("Invalid or unsupported MP3: " + source.getFileName());
        try {
          Files.move(
              temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
          Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
        throw new IOException("Conversion interrupted", e);
      }
    } finally {
      Process p = process;
      if (p != null && p.isAlive()) p.destroyForcibly();
      process = null;
      Files.deleteIfExists(temp);
      Files.deleteIfExists(log);
    }
  }
}
