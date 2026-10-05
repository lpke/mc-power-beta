package local.luke.power.audio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.*;
import paulscode.sound.*;

/** Decoding a seek happens off the game and sound threads, using one bounded PCM buffer. */
public final class MusicSeeking {
  private static final ExecutorService WORKER =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "Power Beta music seek");
            t.setDaemon(true);
            return t;
          });
  private static final AtomicLong GENERATION = new AtomicLong();
  private static final Map<String, Prepared> READY = new HashMap<>();
  private static final Map<String, Double> DURATIONS = new ConcurrentHashMap<>();
  private static final Set<String> READING = ConcurrentHashMap.newKeySet();
  private static CompletableFuture<Prepared> pending;
  private static boolean registered;

  private MusicSeeking() {}

  public record Prepared(
      String id, String source, double seconds, ICodec codec, SoundBuffer first) {}

  public static synchronized void cancel() {
    GENERATION.incrementAndGet();
    if (pending != null)
      pending.thenAccept(
          p -> {
            if (p != null) p.codec.cleanup();
          });
    pending = null;
    READY.values().forEach(p -> p.codec.cleanup());
    READY.clear();
  }

  public static synchronized void request(
      String id, String source, URL url, String filename, double seconds) {
    cancel();
    if (!registered) {
      SoundSystemConfig.setCodec("pbseek", SeekCodec.class);
      registered = true;
    }
    long generation = GENERATION.get();
    pending =
        CompletableFuture.supplyAsync(
            () -> prepare(id, source, url, filename, seconds, generation), WORKER);
  }

  private static Prepared prepare(
      String id, String source, URL url, String filename, double seconds, long generation) {
    ICodec codec = SoundSystemConfig.getCodec(filename);
    if (codec == null) return null;
    try {
      codec.reverseByteOrder(false);
      if (!codec.initialize(url)) {
        codec.cleanup();
        return null;
      }
      AudioFormat format = codec.getAudioFormat();
      long bytes = (long) (Math.max(0, seconds) * format.getFrameRate()) * format.getFrameSize();
      long skipped = 0;
      int empty = 0;
      while (!codec.endOfStream() && GENERATION.get() == generation) {
        SoundBuffer buffer = codec.read();
        if (buffer == null || buffer.audioData == null || buffer.audioData.length == 0) {
          if (++empty > 32) break;
          continue;
        }
        empty = 0;
        if (skipped + buffer.audioData.length > bytes) {
          int offset = (int) (bytes - skipped);
          byte[] remainder = Arrays.copyOfRange(buffer.audioData, offset, buffer.audioData.length);
          return new Prepared(
              id,
              source,
              bytes / (double) format.getFrameSize() / format.getFrameRate(),
              codec,
              new SoundBuffer(remainder, format));
        }
        skipped += buffer.audioData.length;
      }
    } catch (RuntimeException e) {
      local.luke.power.PowerBeta.LOG.warn("Could not seek music", e);
    }
    codec.cleanup();
    return null;
  }

  public static synchronized Prepared take() {
    if (pending == null || !pending.isDone()) return null;
    try {
      return pending.join();
    } catch (CompletionException e) {
      local.luke.power.PowerBeta.LOG.warn("Could not prepare music seek", e);
      return null;
    } finally {
      pending = null;
    }
  }

  public static synchronized boolean busy() {
    return pending != null;
  }

  public static synchronized URL publish(Prepared prepared) {
    String token = UUID.randomUUID().toString();
    READY.put(token, prepared);
    try {
      return new URL("file", "", "/" + token);
    } catch (MalformedURLException e) {
      READY.remove(token);
      prepared.codec.cleanup();
      throw new IllegalArgumentException(e);
    }
  }

  static synchronized Prepared claim(URL url) {
    return READY.remove(url.getPath().substring(1));
  }

  public static double duration(String id, URL url, String filename) {
    Double known = DURATIONS.get(id);
    if (known != null) return known;
    if (READING.add(id))
      WORKER.execute(
          () -> {
            double seconds = 0;
            try {
              if (filename.toLowerCase(Locale.ROOT).endsWith(".ogg")) seconds = oggDuration(url);
              else
                try (AudioInputStream stream = AudioSystem.getAudioInputStream(url)) {
                  if (stream.getFrameLength() > 0)
                    seconds = stream.getFrameLength() / stream.getFormat().getFrameRate();
                }
            } catch (Exception ignored) {
              /* Unknown duration disables seeking, never playback. */
            }
            DURATIONS.put(id, Math.max(0, seconds));
            READING.remove(id);
          });
    return 0;
  }

  static double oggDuration(URL url) throws IOException {
    long last = 0;
    int rate = 0, serial = 0;
    try (InputStream in = new BufferedInputStream(url.openStream())) {
      for (; ; ) {
        byte[] header = in.readNBytes(27);
        if (header.length == 0) break;
        if (header.length != 27
            || header[0] != 'O'
            || header[1] != 'g'
            || header[2] != 'g'
            || header[3] != 'S') throw new IOException("Invalid OGG page");
        ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int stream = h.getInt(14), count = header[26] & 255;
        byte[] segments = in.readNBytes(count);
        if (segments.length != count) throw new EOFException();
        int size = 0;
        for (byte b : segments) size += b & 255;
        byte[] data = in.readNBytes(size);
        if (data.length != size) throw new EOFException();
        if (rate == 0 && data.length >= 16 && data[0] == 1 && data[1] == 'v' && data[2] == 'o') {
          rate = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
          serial = stream;
        }
        if (stream == serial) last = Math.max(last, h.getLong(6));
      }
    }
    return rate > 0 ? last / (double) rate : 0;
  }
}
