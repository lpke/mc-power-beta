package local.luke.power.world;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.FileTransaction;
import local.luke.power.rules.*;
import net.minecraft.nbt.*;

/** Metadata backup and verification, only at world open and explicit saves. */
public final class WorldRuleFiles {
  private WorldRuleFiles() {}
  public static void beforeOpen(Path game, String folder) throws IOException {
    Path saves = game.resolve("saves").toAbsolutePath().normalize();
    Path world = saves.resolve(folder).normalize();
    if (!world.startsWith(saves) || world.equals(saves)) throw new IOException("Invalid world folder.");
    checkPath(world);
    Path level = world.resolve("level.dat"), old = world.resolve("level.dat_old");
    Path source = Files.exists(level) ? level : old;
    if (!Files.exists(source)) return;
    checkPath(source);
    NbtCompound data = read(source).getCompound("Data");
    WorldRuleData.read(data); // Unsupported/ambiguous metadata blocks before session.lock or chunks are touched.
    if (!data.contains(WorldRuleData.TAG)) {
      try (FileTransaction backup = FileTransaction.begin(game, List.of(level, old))) { backup.commit(); }
    }
  }
  public static NbtCompound read(Path path) throws IOException {
    if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("World metadata exceeds 4 MiB: " + path.getFileName());
    try (InputStream input = Files.newInputStream(path)) {
      NbtCompound root = NbtIo.readCompressed(input);
      if (root == null || !root.contains("Data")) throw new IOException("World metadata has no Data compound.");
      return root;
    } catch (RuntimeException e) { throw new IOException("Invalid world metadata " + path.getFileName() + ": " + e.getMessage(), e); }
  }
  public static void verifySaved(Path world, RuleSet expected, int difficulty) throws IOException {
    Path file = world.resolve("level.dat"); checkPath(file);
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) { channel.force(true); }
    local.luke.power.storage.DurableFiles.syncDirectory(world);
    NbtCompound data = read(file).getCompound("Data");
    if (!data.contains(WorldRuleData.TAG) || !WorldRuleData.read(data).equals(expected)
        || data.getInt(WorldDifficulty.TAG) != difficulty)
      throw new IOException("The saved world rules could not be verified. The previous settings will be restored.");
  }
  private static void checkPath(Path path) throws IOException {
    for (Path part = path; part != null; part = part.getParent())
      if (Files.isSymbolicLink(part)) throw new IOException("World metadata path contains a symbolic link.");
  }
}
