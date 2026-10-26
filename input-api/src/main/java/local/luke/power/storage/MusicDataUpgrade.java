package local.luke.power.storage;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

/** Explicit format-2 conversion. Call only after confirming the instance is idle. */
public final class MusicDataUpgrade {
  private MusicDataUpgrade() {}

  @FunctionalInterface
  interface Faults { void at(String stage, Path file) throws IOException; }

  /** Preserve list order and every value except the retired source Off flags. */
  public static JsonObject convert(JsonObject previous) {
    MusicData.validatePrevious(previous);
    JsonObject next = previous.deepCopy();
    next.addProperty("formatVersion", MusicData.VERSION);
    JsonObject audio = next.getAsJsonObject("audio");
    audio.remove("disabledMusicDirectories");
    MusicData.validate(next);
    return next;
  }

  /** Return the durable, byte-verified backup. The shared configuration is never edited. */
  public static Path convert(Path musicFile) throws IOException {
    return convert(musicFile, (stage, file) -> {});
  }

  static Path convert(Path musicFile, Faults faults) throws IOException {
    Path file = musicFile.toAbsolutePath().normalize();
    MusicData.checkPath(file);
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > MusicData.MAX_BYTES)
      throw new IOException("Unsafe, missing or oversized music data; original file preserved");
    byte[] original = Files.readAllBytes(file);
    JsonObject next;
    try {
      next = convert(JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject());
    } catch (RuntimeException failure) {
      throw new IOException("Music conversion refused; original file preserved: " + failure.getMessage(), failure);
    }
    byte[] converted = MusicData.bytes(next);
    Path backup = MusicData.backup(file, "music-format-2-");
    if (!Arrays.equals(original, Files.readAllBytes(backup)))
      throw new IOException("Music changed before backup completed; original file preserved");
    Path temporary = Files.createTempFile(file.getParent(), ".music-upgrade-", ".tmp");
    boolean replaced = false;
    try {
      MusicData.durableWrite(temporary, converted);
      MusicData.read(temporary);
      if (!Arrays.equals(original, Files.readAllBytes(file)))
        throw new IOException("Music changed during conversion; original file preserved");
      Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      replaced = true;
      faults.at("replaced", file);
      DurableFiles.syncDirectory(file.getParent());
      if (!Arrays.equals(converted, Files.readAllBytes(file)))
        throw new IOException("Converted music verification failed; restore " + backup);
      MusicData.read(file);
      return backup;
    } catch (IOException | RuntimeException failure) {
      if (!replaced) throw failure;
      String recovery;
      try {
        restoreOwned(file, backup, original, converted, faults);
        recovery = "Original music data restored.";
      } catch (IOException | RuntimeException rollbackFailure) {
        failure.addSuppressed(rollbackFailure);
        recovery = "Automatic rollback could not complete. Preserve the current file and restore the backup after checking for concurrent changes.";
      }
      throw new IOException("Music conversion failed after replacement. " + recovery
          + " Verified backup: " + backup, failure);
    } finally { Files.deleteIfExists(temporary); }
  }

  private static void restoreOwned(Path file, Path backup, byte[] original, byte[] converted,
      Faults faults) throws IOException {
    MusicData.checkPath(backup);
    if (!Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS)
        || Files.size(backup) != original.length || !Arrays.equals(original, Files.readAllBytes(backup)))
      throw new IOException("Original music backup could not be verified: " + backup);
    requireOwned(file, converted);
    Path temporary = Files.createTempFile(file.getParent(), ".music-rollback-", ".tmp");
    try {
      MusicData.durableWrite(temporary, original);
      faults.at("rollback-prepared", file);
      requireOwned(file, converted);
      Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      DurableFiles.syncDirectory(file.getParent());
      if (!Arrays.equals(original, Files.readAllBytes(file)))
        throw new IOException("Restored music verification failed; backup retained at " + backup);
    } finally { Files.deleteIfExists(temporary); }
  }

  private static void requireOwned(Path file, byte[] converted) throws IOException {
    MusicData.checkPath(file);
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
        || Files.size(file) != converted.length || !Arrays.equals(converted, Files.readAllBytes(file)))
      throw new IOException("Music changed after replacement; rollback will not overwrite another writer's data");
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2 || !args[0].equals("--idle-instance"))
      throw new IllegalArgumentException("Usage: MusicDataUpgrade --idle-instance /path/to/power-beta/music.json");
    System.out.println("Music data converted to format 3. Verified backup: " + convert(Path.of(args[1])));
  }
}
