package local.luke.power.world;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import local.luke.power.config.FileTransaction;
import local.luke.power.storage.DurableFiles;

/** Before/after evidence permits rollback only while the transaction still owns the bytes. */
public final class OwnedMetadataTransaction implements AutoCloseable {
  private static final long MAX_BYTES = 64L * 1024 * 1024;
  private static final int MAX_FILE = 4 * 1024 * 1024;
  private final Path game, directory;
  private final JsonObject manifest;
  private final Map<Path, byte[]> before, after;
  private final Set<Path> armed = new LinkedHashSet<>();
  private boolean complete;

  private OwnedMetadataTransaction(Path game, Path directory, JsonObject manifest,
      Map<Path, byte[]> before, Map<Path, byte[]> after) {
    this.game = game; this.directory = directory; this.manifest = manifest;
    this.before = before; this.after = after;
  }

  public static OwnedMetadataTransaction begin(Path game, Map<Path, byte[]> expected, Map<Path, byte[]> planned)
      throws IOException {
    game = game.toAbsolutePath().normalize();
    if (planned.isEmpty() || planned.size() > 256 || !expected.keySet().containsAll(planned.keySet()))
      throw new IOException("Invalid metadata transaction.");
    var before = new LinkedHashMap<Path, byte[]>(); var after = new LinkedHashMap<Path, byte[]>();
    long total = 0;
    for (var entry : planned.entrySet()) {
      Path path = entry.getKey().toAbsolutePath().normalize(); safeTarget(game, path);
      byte[] old = expected.get(entry.getKey()), next = entry.getValue();
      if (old != null && old.length > MAX_FILE || next == null || next.length > MAX_FILE)
        throw new IOException("Metadata transaction exceeds its file limit.");
      total += (old == null ? 0 : old.length) + next.length;
      if (total > MAX_BYTES) throw new IOException("Metadata transaction exceeds 64 MiB.");
      before.put(path, old == null ? null : old.clone()); after.put(path, next.clone());
    }
    requireBefore(before);
    Path root = game.resolve("power-beta-data/world-metadata-backups");
    SavedWorldAccess.safe(root); DurableFiles.createDirectories(root);
    Path directory = Files.createTempDirectory(root, "upgrade-");
    JsonObject manifest = new JsonObject(); manifest.addProperty("format", 1); manifest.addProperty("state", "prepared");
    JsonArray files = new JsonArray(); int index = 0;
    for (Path path : before.keySet()) {
      JsonObject file = new JsonObject(); file.addProperty("path", game.relativize(path).toString()); file.addProperty("armed", false);
      for (boolean previous : new boolean[]{true, false}) {
        byte[] bytes = previous ? before.get(path) : after.get(path);
        String key = previous ? "before" : "after";
        if (bytes == null) { file.add(key, JsonNull.INSTANCE); continue; }
        String name = index + "." + key;
        FileTransaction.atomicWrite(directory.resolve(name), bytes);
        if (!Arrays.equals(bytes, Files.readAllBytes(directory.resolve(name)))) throw new IOException("Metadata backup verification failed.");
        file.addProperty(key, name); file.addProperty(key + "Sha256", hash(bytes));
      }
      files.add(file); index++;
    }
    manifest.add("files", files);
    writeManifest(directory, manifest); DurableFiles.syncDirectory(root);
    return new OwnedMetadataTransaction(game, directory, manifest, before, after);
  }

  public void apply() throws IOException {
    requireBefore(before);
    for (var entry : after.entrySet()) {
      requireBefore(Collections.singletonMap(entry.getKey(), before.get(entry.getKey())));
      String relative = game.relativize(entry.getKey()).toString();
      for (JsonElement value : manifest.getAsJsonArray("files")) {
        JsonObject file = value.getAsJsonObject();
        if (file.get("path").getAsString().equals(relative)) file.addProperty("armed", true);
      }
      writeManifest(directory, manifest); armed.add(entry.getKey());
      requireBefore(Collections.singletonMap(entry.getKey(), before.get(entry.getKey())));
      FileTransaction.atomicWrite(entry.getKey(), entry.getValue());
      if (!Arrays.equals(read(entry.getKey()), entry.getValue())) throw new IOException("Metadata write verification failed.");
    }
  }

  public void commit() throws IOException {
    for (var entry : after.entrySet()) if (!Arrays.equals(read(entry.getKey()), entry.getValue()))
      throw new IOException("Metadata changed before transaction completion.");
    manifest.addProperty("state", "complete"); writeManifest(directory, manifest); complete = true;
  }

  @Override public void close() throws IOException {
    if (!complete) rollback(directory, manifest, before, after, armed);
  }

  /** Called before opening or editing this world, never on input/render paths. */
  public static void recover(Path game, Path world) throws IOException {
    game = game.toAbsolutePath().normalize(); world = world.toAbsolutePath().normalize();
    Path root = game.resolve("power-beta-data/world-metadata-backups"); SavedWorldAccess.safe(root);
    if (!Files.exists(root)) return;
    try (var paths = Files.list(root)) {
      for (Path directory : paths.filter(Files::isDirectory).sorted().toList()) {
        SavedWorldAccess.safe(directory); Path marker = directory.resolve("transaction.json");
        if (!Files.exists(marker)) continue; // Evidence prepared before the write intent cannot have published.
        if (Files.size(marker) > 256 * 1024) throw new IOException("Metadata recovery manifest is too large.");
        JsonObject manifest;
        try { manifest = JsonParser.parseString(Files.readString(marker)).getAsJsonObject(); }
        catch (RuntimeException error) { throw new IOException("Invalid metadata recovery manifest. Preserve its backup.", error); }
        String state = manifest.has("state") ? manifest.get("state").getAsString() : "";
        if (state.equals("complete") || state.equals("recovered")) continue;
        if (!manifest.has("format") || manifest.get("format").getAsInt() != 1 || !manifest.has("files")
            || !manifest.get("files").isJsonArray() || manifest.getAsJsonArray("files").size() > 256)
          throw new IOException("Unsupported metadata recovery format. Use a compatible Power Beta release.");
        var before = new LinkedHashMap<Path, byte[]>(); var after = new LinkedHashMap<Path, byte[]>();
        var armed = new LinkedHashSet<Path>();
        long total = 0; boolean relevant = false;
        for (JsonElement value : manifest.getAsJsonArray("files")) {
          JsonObject file = value.getAsJsonObject(); Path path = game.resolve(file.get("path").getAsString()).normalize();
          safeTarget(game, path); relevant |= path.startsWith(world);
          if (before.containsKey(path)) throw new IOException("Duplicate metadata recovery target.");
          if (!file.has("armed") || !file.get("armed").isJsonPrimitive() || !file.getAsJsonPrimitive("armed").isBoolean())
            throw new IOException("Missing metadata write-intent state.");
          if (file.get("armed").getAsBoolean()) armed.add(path);
          byte[] previous = evidence(directory, file, "before"), next = evidence(directory, file, "after");
          if (next == null) throw new IOException("Metadata recovery has no intended result.");
          total += (previous == null ? 0 : previous.length) + next.length;
          if (total > MAX_BYTES) throw new IOException("Metadata recovery exceeds 64 MiB.");
          before.put(path, previous); after.put(path, next);
        }
        if (relevant) rollback(directory, manifest, before, after, armed);
      }
    } catch (RuntimeException error) { throw new IOException("Invalid metadata recovery. Preserve the original files and backups.", error); }
  }

  private static void rollback(Path directory, JsonObject manifest, Map<Path, byte[]> before, Map<Path, byte[]> after, Set<Path> armed)
      throws IOException {
    // Check the entire rollback first. A foreign edit never gets overwritten, even in another target.
    for (Path path : armed) {
      byte[] current = read(path);
      if (!Arrays.equals(current, before.get(path)) && !Arrays.equals(current, after.get(path))) {
        manifest.addProperty("state", "blocked"); writeManifest(directory, manifest);
        throw new IOException("World metadata changed outside Power Beta. No recovery files were overwritten. Preserve world-metadata-backups and restore a verified matching backup.");
      }
    }
    for (Path path : armed) if (!Arrays.equals(read(path), before.get(path))) {
      // Recheck ownership immediately before each replacement as well.
      if (!Arrays.equals(read(path), after.get(path))) throw new IOException("World metadata changed during recovery; originals preserved.");
      if (before.get(path) == null) { Files.delete(path); DurableFiles.syncDirectory(path.getParent()); }
      else FileTransaction.atomicWrite(path, before.get(path));
      if (!Arrays.equals(read(path), before.get(path))) throw new IOException("Metadata rollback verification failed.");
    }
    manifest.addProperty("state", "recovered"); writeManifest(directory, manifest);
  }

  private static byte[] evidence(Path directory, JsonObject file, String key) throws IOException {
    if (!file.has(key)) throw new IOException("Incomplete metadata recovery record.");
    if (file.get(key).isJsonNull()) return null;
    String name = file.get(key).getAsString();
    if (!name.matches("[0-9]+\\.(before|after)")) throw new IOException("Invalid metadata evidence path.");
    byte[] bytes = read(directory.resolve(name));
    if (bytes == null || !file.has(key + "Sha256") || !hash(bytes).equals(file.get(key + "Sha256").getAsString()))
      throw new IOException("Metadata backup checksum failed. Original files are preserved.");
    return bytes;
  }
  private static void writeManifest(Path directory, JsonObject manifest) throws IOException {
    FileTransaction.atomicWrite(directory.resolve("transaction.json"), manifest.toString().getBytes(StandardCharsets.UTF_8));
  }
  private static void requireBefore(Map<Path, byte[]> expected) throws IOException {
    for (var entry : expected.entrySet()) if (!Arrays.equals(read(entry.getKey()), entry.getValue()))
      throw new IOException("World metadata changed. Reopen the editor before saving.");
  }
  private static byte[] read(Path path) throws IOException {
    SavedWorldAccess.safe(path);
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > MAX_FILE)
      throw new IOException("Invalid metadata file or file exceeds 4 MiB.");
    byte[] bytes = Files.readAllBytes(path);
    if (bytes.length > MAX_FILE) throw new IOException("Metadata file changed beyond 4 MiB.");
    return bytes;
  }
  private static void safeTarget(Path game, Path path) throws IOException {
    if (!path.startsWith(game.resolve("saves")) || !Set.of("level.dat", "level.dat_old").contains(path.getFileName().toString()))
      throw new IOException("Metadata recovery target is outside saved worlds.");
    SavedWorldAccess.safe(path);
  }
  private static String hash(byte[] bytes) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
}
