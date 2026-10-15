package local.luke.power.storage;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Shared by every owned module. No module owns a second settings file. */
public final class PowerConfig {
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Object DISK = new Object();
  private static final DeferredSaves DEFERRED = new DeferredSaves(PowerConfig::writeDeferred);
  private static Path file;
  private static JsonObject recoveryDefaults;
  private static String problem = "";
  /** Only bootstrap may enter recovery mode. Defaults remain in memory and writes stay blocked. */
  public static void recovery(JsonObject defaults, String message) {
    synchronized (DISK) { recoveryDefaults = ConfigVersions.convert(defaults); problem = message; }
  }
  public static String problem() { synchronized (DISK) { return problem; } }
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
  public static JsonObject document() {
    try { flushPending(); }
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
    flushPending();
    synchronized (DISK) {
      JsonObject root = readDocument(); root.getAsJsonObject("settings").add(id, value.deepCopy()); writeDocument(root);
    }
  }
  public static void write(JsonObject root) throws IOException {
    flushPending();
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
    byte[] bytes = (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    if (bytes.length > 4 * 1024 * 1024) throw new IOException("Configuration exceeds 4 MiB");
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
  public static <T> T read(String id, Class<T> type) {
    try { return JSON.fromJson(section(id), type); }
    catch (RuntimeException e) { throw new IllegalStateException("Invalid settings section " + id, e); }
  }
  public static void save(String id, Object value) throws IOException { put(id, JSON.toJsonTree(value).getAsJsonObject()); }

  /** Gameplay changes take effect immediately; only owned snapshots reach the worker. */
  public static void saveDeferred(String id, Object value) {
    DEFERRED.submit(id, JSON.toJsonTree(value).getAsJsonObject());
  }
  public static void flushPending() throws IOException { DEFERRED.flush(); }
  public static IOException takeSaveFailure() { return DEFERRED.takeFailure(); }

  private static void writeDeferred(java.util.Map<String, JsonObject> changes) throws IOException {
    synchronized (DISK) {
      JsonObject root = readDocument(), settings = root.getAsJsonObject("settings");
      changes.forEach((id, value) -> settings.add(id, value.deepCopy()));
      writeDocument(root);
    }
  }
}
