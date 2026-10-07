package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Durable backup before any settings writer runs. A pending save is recovered before mod init. */
public final class FileTransaction implements AutoCloseable {
  private final Path game, dir;
  private final JsonObject manifest;
  private boolean committed;

  private FileTransaction(Path game, Path dir, JsonObject manifest) {
    this.game = game;
    this.dir = dir;
    this.manifest = manifest;
  }

  public static FileTransaction begin(Path game, List<Path> files) throws IOException {
    game = game.toAbsolutePath().normalize();
    Path root = game.resolve("power-beta-data/settings-backups");
    checkPath(game, root);
    Files.createDirectories(root);
    Path dir = Files.createTempDirectory(root, "save-");
    JsonObject manifest = new JsonObject();
    int index = 0;
    for (Path file : files) {
      Path path = file.toAbsolutePath().normalize();
      checkPath(game, path);
      String relative = game.relativize(path).toString();
      if (Files.exists(path)) {
        if (Files.size(path) > 4 * 1024 * 1024)
          throw new IOException("Settings file exceeds 4 MiB: " + relative);
        String backup = "file-" + (index++);
        durableWrite(dir.resolve(backup), Files.readAllBytes(path));
        manifest.addProperty(relative, backup);
      } else manifest.add(relative, JsonNull.INSTANCE);
    }
    durableWrite(
        dir.resolve("pending.json"),
        Catalog.JSON.toJson(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return new FileTransaction(game, dir, manifest);
  }

  public void commit() throws IOException {
    Files.move(
        dir.resolve("pending.json"), dir.resolve("complete.json"), StandardCopyOption.ATOMIC_MOVE);
    committed = true;
  }

  public void close() throws IOException {
    if (!committed) {
      restore(game, dir, manifest);
      Files.move(
          dir.resolve("pending.json"),
          dir.resolve("recovered.json"),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static void recover(Path game) throws IOException {
    game = game.toAbsolutePath().normalize();
    recoverRoot(game, game.resolve("power-beta-data/settings-backups"));
  }

  private static void recoverRoot(Path game, Path root) throws IOException {
    checkPath(game, root);
    if (!Files.isDirectory(root)) return;
    try (var dirs = Files.list(root)) {
      for (Path dir : dirs.sorted().toList()) {
        Path pending = dir.resolve("pending.json");
        checkPath(game, pending);
        if (!Files.exists(pending)) continue;
        JsonObject data = JsonParser.parseString(Files.readString(pending)).getAsJsonObject();
        restore(game.toAbsolutePath().normalize(), dir, data);
        Files.move(pending, dir.resolve("recovered.json"), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private static void restore(Path game, Path dir, JsonObject data) throws IOException {
    for (var e : data.entrySet()) {
      Path destination = game.resolve(e.getKey()).normalize();
      checkPath(game, destination);
      if (e.getValue().isJsonNull()) Files.deleteIfExists(destination);
      else {
        Path source = dir.resolve(e.getValue().getAsString()).normalize();
        checkPath(dir, source);
        if (!Files.isRegularFile(source)) throw new IOException("Invalid settings backup");
        atomicWrite(destination, Files.readAllBytes(source));
      }
    }
  }

  private static void checkPath(Path root, Path path) throws IOException {
    if (!path.startsWith(root) || path.equals(root))
      throw new IOException("Settings path is outside the instance");
    for (Path part = path; part != null && !part.equals(root); part = part.getParent())
      if (Files.isSymbolicLink(part))
        throw new IOException("Settings path contains a symbolic link: " + part.getFileName());
  }

  public static void atomicWrite(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Path tmp = Files.createTempFile(path.getParent(), "power-beta-", ".tmp");
    try {
      durableWrite(tmp, bytes);
      try {
        Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private static void durableWrite(Path path, byte[] bytes) throws IOException {
    Files.write(path, bytes);
    try (var ch = FileChannel.open(path, StandardOpenOption.WRITE)) {
      ch.force(true);
    }
  }
}
