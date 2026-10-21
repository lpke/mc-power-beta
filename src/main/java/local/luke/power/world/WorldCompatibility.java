package local.luke.power.world;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.List;
import local.luke.power.input.WorldIdentity;
import local.luke.power.rules.RuleSet;
import local.luke.power.storage.DurableFiles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** A load-boundary durability proof. Gameplay queries never acquire a disk lock or read a file. */
public final class WorldCompatibility {
  private static World verified;
  private WorldCompatibility() {}
  public static boolean ready(World world) { return world != null && world == verified; }
  public static void clear() {
    if (verified != null) ((WorldIdentity)verified.method_262()).power$inventoryReady(false);
    verified = null;
  }

  public static void confirm(Minecraft mc) throws IOException {
    clear();
    if (mc.world == null || mc.world.isRemote) return;
    Path directory = WorldRuleChanges.directory(mc).toAbsolutePath().normalize();
    Path game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    if (!game.resolve("saves").equals(directory.getParent())) throw new IOException("Unexpected loaded world storage path.");
    // Fresh native worlds save once at the end of loading. Verify that save before enabling new ownership.
    WorldRuleFiles.beforeOpen(game, directory.getFileName().toString());
    if (!Files.isRegularFile(directory.resolve("level.dat"))) throw new IOException("The new world's initial metadata save is missing. New inventory features remain blocked until a verified save exists.");
    String expected = ((WorldIdentity)mc.world.method_262()).power$commandWorldId();
    for (String name : List.of("level.dat", "level.dat_old")) {
      Path path = directory.resolve(name);
      if (!Files.exists(path)) continue;
      var data = WorldRuleFiles.read(path).getCompound("Data");
      if (data.getCompound(WorldRuleData.TAG).getInt("version") != RuleSet.VERSION
          || !expected.equals(data.getString("PowerBetaCommandWorldId")))
        throw new IOException("World compatibility or recovery identity could not be verified. Preserve the metadata backups before continuing.");
      try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); }
    }
    DurableFiles.syncDirectory(directory);
    ((WorldIdentity)mc.world.method_262()).power$inventoryReady(true);
    verified = mc.world;
  }
}
