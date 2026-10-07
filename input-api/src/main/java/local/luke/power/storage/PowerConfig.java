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
  private static Path file;
  public static synchronized void configure(Path path) {
    Path next = path.toAbsolutePath().normalize();
    if (file != null && !file.equals(next)) throw new IllegalStateException("Configuration path changed");
    file = next;
  }
  public static synchronized Path path() {
    if (file == null) throw new IllegalStateException("Configuration was not initialized");
    return file;
  }
  public static synchronized JsonObject document() {
    Path path = path();
    try {
      if (!Files.exists(path)) { JsonObject root = new JsonObject(); root.addProperty("schemaVersion", 3); root.add("settings", new JsonObject()); return root; }
      if (Files.isSymbolicLink(path) || Files.size(path) > 4 * 1024 * 1024) throw new IOException("Unsafe configuration file");
      JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
      if (root.get("schemaVersion").getAsInt() != 3 || !root.get("settings").isJsonObject()) throw new IOException("Unsupported configuration schema");
      return root;
    } catch (Exception e) { throw new IllegalStateException("Cannot safely read " + path + "; original file preserved", e); }
  }
  public static synchronized JsonObject section(String id) {
    JsonElement value = document().getAsJsonObject("settings").get(id);
    return value == null ? new JsonObject() : value.getAsJsonObject().deepCopy();
  }
  public static synchronized void put(String id, JsonObject value) throws IOException {
    JsonObject root = document(); root.getAsJsonObject("settings").add(id, value.deepCopy()); write(root);
  }
  public static synchronized void write(JsonObject root) throws IOException {
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
      try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) { directory.force(true); }
    } finally { Files.deleteIfExists(temp); }
  }
  public static synchronized <T> T read(String id, Class<T> type) {
    try { return JSON.fromJson(section(id), type); }
    catch (RuntimeException e) { throw new IllegalStateException("Invalid settings section " + id, e); }
  }
  public static synchronized void save(String id, Object value) throws IOException { put(id, JSON.toJsonTree(value).getAsJsonObject()); }
}
