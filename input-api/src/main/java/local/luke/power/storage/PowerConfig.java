package local.luke.power.storage;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Shared settings and user-data writer for every owned module. */
public final class PowerConfig {
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Object DISK = new Object();
  private static final DeferredSaves DEFERRED = new DeferredSaves(PowerConfig::writeDeferred);
  private static final DeferredSaves MUSIC_DEFERRED = new DeferredSaves(changes -> {
    synchronized (DISK) { writeMusicQueue(changes.get("queue")); }
  });
  private static Path file;
  private static JsonObject recoveryDefaults;
  private static volatile String problem = "";
  /** Only bootstrap may enter recovery mode. Defaults remain in memory and writes stay blocked. */
  public static void recovery(JsonObject defaults, String message) {
    synchronized (DISK) { recoveryDefaults = ConfigVersions.convert(defaults); problem = message; }
  }
  /** Read on every game tick. Never join the durable writer's disk lock here. */
  public static String problem() { return problem; }
  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try { flushPending(); }
      catch (IOException e) { System.err.println("Power Beta settings could not be saved on shutdown: " + e); }
    }, "Power Beta settings shutdown"));
  }
  public static synchronized void configure(Path path) {
    Path next = path.toAbsolutePath().normalize();
    if (file != null && !file.equals(next)) throw new IllegalStateException("Configuration path changed");
    file = next;
  }
  public static synchronized Path path() {
    if (file == null) throw new IllegalStateException("Configuration was not initialized");
    return file;
  }
  public static Path musicPath() { return MusicData.path(path()); }

  /** Fresh installs only. Existing music data is always validated and never replaced. */
  public static void initializeMusicData() throws IOException {
    flushPending();
    synchronized (DISK) {
      if (recoveryDefaults != null) throw new IOException("Music data is read-only during configuration recovery");
      JsonObject config = readDocument();
      if (Files.exists(musicPath(), LinkOption.NOFOLLOW_LINKS)) MusicData.read(musicPath());
      else {
        JsonObject empty = MusicData.empty();
        MusicData.validateSelections(audioPreferences(config), empty.getAsJsonObject("audio"));
        MusicData.write(musicPath(), empty);
      }
    }
  }

  private static JsonObject musicData(JsonObject config) throws IOException {
    if (recoveryDefaults != null) throw new IOException("Music data is read-only during configuration recovery");
    JsonObject data = MusicData.read(musicPath());
    JsonObject preferences = audioPreferences(config);
    MusicData.validatePreferences(preferences);
    MusicData.validateSelections(preferences, data.getAsJsonObject("audio"));
    return data;
  }

  private static JsonObject audioPreferences(JsonObject config) {
    JsonElement audio = config.getAsJsonObject("settings").get("audio");
    return audio == null ? new JsonObject() : audio.getAsJsonObject();
  }

  public static JsonObject audio() {
    try {
      flushPending();
      synchronized (DISK) {
        JsonObject config = readDocument(), data = musicData(config);
        JsonObject preferences = audioPreferences(config);
        return MusicData.merge(preferences, data.getAsJsonObject("audio"));
      }
    } catch (IOException failure) { throw new IllegalStateException("Cannot safely read music data; original files preserved", failure); }
  }

  public static <T> T readAudio(Class<T> type) { return JSON.fromJson(audio(), type); }

  /** Options transactions include both paths; a stale Options draft never contains the queue. */
  public static void saveAudio(Object value) throws IOException {
    JsonObject next = JSON.toJsonTree(value).getAsJsonObject();
    flushPending();
    synchronized (DISK) {
      JsonObject data = musicData(readDocument()), config = readDocument();
      JsonObject preferences = MusicData.preferences(next), music = MusicData.split(next);
      MusicData.validateSelections(preferences, music);
      data.add("audio", music); MusicData.validate(data);
      config.getAsJsonObject("settings").add("audio", preferences);
      configurationBytes(config); MusicData.bytes(data);
      MusicData.backup(path(), "power-beta-audio-");
      MusicData.backup(musicPath(), "music-audio-");
      MusicData.write(musicPath(), data); writeDocument(config);
    }
  }

  public static JsonObject musicQueue() {
    try {
      flushPending();
      synchronized (DISK) { return musicData(readDocument()).getAsJsonObject("queue").deepCopy(); }
    } catch (IOException failure) { throw new IllegalStateException("Cannot safely read the music queue", failure); }
  }
  public static <T> T readMusicQueue(Class<T> type) { return JSON.fromJson(musicQueue(), type); }
  public static void saveMusicQueue(Object value) throws IOException {
    JsonObject queue = JSON.toJsonTree(value).getAsJsonObject(); MusicData.validateQueue(queue);
    flushPending(); synchronized (DISK) { writeMusicQueue(queue); }
  }
  public static void saveMusicQueueDeferred(Object value) {
    JsonObject queue = JSON.toJsonTree(value).getAsJsonObject(); MusicData.validateQueue(queue);
    MUSIC_DEFERRED.submit("queue", queue);
  }
  private static void writeMusicQueue(JsonObject queue) throws IOException {
    JsonObject data = musicData(readDocument()); data.add("queue", queue.deepCopy()); MusicData.write(musicPath(), data);
  }
  public static JsonObject document() {
    try { DEFERRED.flush(); }
    catch (IOException e) { throw new IllegalStateException("Cannot finish saving settings; unsaved changes retained", e); }
    synchronized (DISK) { return readDocument(); }
  }
  private static JsonObject readDocument() {
    if (recoveryDefaults != null) return recoveryDefaults.deepCopy();
    Path path = path();
    try {
      for (Path part = path; part != null; part = part.getParent())
        if (Files.isSymbolicLink(part)) throw new IOException("Unsafe configuration path");
      if (!Files.exists(path)) { JsonObject root = new JsonObject(); root.addProperty("schemaVersion", ConfigVersions.CURRENT); root.add("settings", new JsonObject()); return root; }
      if (Files.isSymbolicLink(path) || Files.size(path) > 4 * 1024 * 1024) throw new IOException("Unsafe configuration file");
      byte[] original = Files.readAllBytes(path);
      JsonObject root = JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject();
      JsonObject converted = ConfigVersions.convert(root);
      MusicData.validatePreferences(audioPreferences(converted));
      if (!converted.equals(root)) {
        backup(original, ConfigVersions.version(root));
        writeDocument(converted);
      }
      return converted;
    } catch (Exception e) { throw new IllegalStateException("Cannot safely read " + path + ": " + e.getMessage() + "; original file preserved", e); }
  }
  public static JsonObject section(String id) {
    JsonElement value = document().getAsJsonObject("settings").get(id);
    return value == null ? new JsonObject() : value.getAsJsonObject().deepCopy();
  }
  public static void put(String id, JsonObject value) throws IOException {
    DEFERRED.flush();
    synchronized (DISK) {
      JsonObject root = readDocument(); root.getAsJsonObject("settings").add(id, value.deepCopy()); writeDocument(root);
    }
  }
  /** Menu/offline maintenance only: reject stale edits and verify a durable original before replacement. */
  public static void replaceSection(String id, JsonElement expected, JsonObject value) throws IOException {
    DEFERRED.flush();
    synchronized (DISK) {
      JsonObject root = readDocument();
      JsonObject settings = root.getAsJsonObject("settings");
      if (!java.util.Objects.equals(expected, settings.get(id)))
        throw new IOException("Settings changed since this editor opened. Reopen it before saving.");
      if (Files.exists(path())) backup(Files.readAllBytes(path()), ConfigVersions.version(root));
      settings.add(id, value.deepCopy());
      writeDocument(ConfigVersions.convert(root));
      JsonObject verified = readDocument();
      if (!value.equals(verified.getAsJsonObject("settings").get(id)))
        throw new IOException("Settings replacement could not be verified. Restore the verified backup.");
    }
  }
  public static void write(JsonObject root) throws IOException {
    DEFERRED.flush();
    synchronized (DISK) {
      if (Files.exists(path(), LinkOption.NOFOLLOW_LINKS)) readDocument();
      writeDocument(ConfigVersions.convert(root));
    }
  }
  private static void backup(byte[] original, int version) throws IOException {
    Path directory = path().getParent().resolve("backups");
    if (Files.isSymbolicLink(directory)) throw new IOException("Unsafe backup directory");
    Files.createDirectories(directory);
    Path backup = Files.createTempFile(directory, "power-beta-v" + version + "-", ".json");
    try (FileChannel out = FileChannel.open(backup, StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(original);
      while (buffer.hasRemaining()) out.write(buffer);
      out.force(true);
    }
    if (!java.util.Arrays.equals(original, Files.readAllBytes(backup)))
      throw new IOException("Configuration backup verification failed");
    DurableFiles.syncDirectory(directory);
  }
  private static void writeDocument(JsonObject root) throws IOException {
    if (!problem.isEmpty()) throw new IOException(problem + " Settings are read-only until the file is repaired and the game is restarted.");
    Path path = path();
    for (Path part = path; part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("Unsafe configuration path");
    byte[] bytes = configurationBytes(root);
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), ".power-beta-", ".tmp");
    try {
      try (FileChannel out = FileChannel.open(temp, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) out.write(buffer); out.force(true);
      }
      Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
      DurableFiles.syncDirectory(path.getParent());
    } finally { Files.deleteIfExists(temp); }
  }
  private static byte[] configurationBytes(JsonObject root) throws IOException {
    ConfigVersions.convert(root);
    MusicData.validatePreferences(audioPreferences(root));
    byte[] bytes = (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    if (bytes.length > 4 * 1024 * 1024) throw new IOException("Configuration exceeds 4 MiB");
    return bytes;
  }
  public static <T> T read(String id, Class<T> type) {
    try { return JSON.fromJson(section(id), type); }
    catch (RuntimeException e) { throw new IllegalStateException("Invalid settings section " + id, e); }
  }
  public static void save(String id, Object value) throws IOException { put(id, JSON.toJsonTree(value).getAsJsonObject()); }

  /** Gameplay changes take effect immediately; only owned snapshots reach the worker. */
  public static void saveDeferred(String id, Object value) {
    DEFERRED.submit(id, JSON.toJsonTree(value).getAsJsonObject());
  }
  public static void flushPending() throws IOException {
    IOException failure = null;
    try { DEFERRED.flush(); } catch (IOException e) { failure = e; }
    try { MUSIC_DEFERRED.flush(); } catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
    if (failure != null) throw failure;
  }
  public static IOException takeSaveFailure() {
    IOException failure = DEFERRED.takeFailure();
    return failure == null ? MUSIC_DEFERRED.takeFailure() : failure;
  }

  private static void writeDeferred(java.util.Map<String, JsonObject> changes) throws IOException {
    synchronized (DISK) {
      JsonObject root = readDocument(), settings = root.getAsJsonObject("settings");
      changes.forEach((id, value) -> settings.add(id, value.deepCopy()));
      writeDocument(root);
    }
  }
}
