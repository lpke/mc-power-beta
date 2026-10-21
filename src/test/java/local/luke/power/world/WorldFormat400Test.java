package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.rules.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldFormat400Test {
  @TempDir Path game;
  private Path save(String relative, int version, String id) throws Exception {
    Path file = game.resolve("saves/Example").resolve(relative); Files.createDirectories(file.getParent());
    NbtCompound root = new NbtCompound(), data = new NbtCompound(), rules = new NbtCompound();
    rules.putInt("version", version); rules.putBoolean("allow_creative_mode", true);
    rules.putBoolean("allow_sprinting", false); rules.putBoolean("keep_inventory", true);
    data.put("GameRules", rules); data.putLong("Time", 4567); data.putInt("PowerBetaDifficulty", 3);
    if (id != null) data.putString("PowerBetaCommandWorldId", id);
    data.putString("Custom", "untouched"); root.put("Data", data); write(file, root); return file;
  }
  private static void write(Path file, NbtCompound root) throws Exception {
    try (var output = Files.newOutputStream(file)) { NbtIo.writeCompressed(root, output); }
  }
  @Test void fencesBothFallbacksAndDimensionsWithoutChangingPermissionsOrOtherData() throws Exception {
    String id = UUID.randomUUID().toString();
    List<Path> files = List.of(save("level.dat", 3, id), save("level.dat_old", 2, id),
        save("DIM-1/level.dat", 3, id), save("DIM-1/level.dat_old", 3, id));
    WorldRuleFiles.beforeOpen(game, "Example");
    for (Path file : files) {
      var data = WorldRuleFiles.read(file).getCompound("Data");
      assertEquals(4, data.getCompound("GameRules").getInt("version"));
      assertEquals(id, data.getString("PowerBetaCommandWorldId"));
      var rules = WorldRuleData.read(data);
      assertTrue(rules.enabled(GameRule.ALLOW_CREATIVE_MODE));
      assertFalse(rules.enabled(GameRule.ALLOW_SPRINTING));
      assertTrue(rules.enabled(GameRule.KEEP_INVENTORY));
      assertEquals(4567, data.getLong("Time")); assertEquals(3, data.getInt("PowerBetaDifficulty"));
      assertEquals("untouched", data.getString("Custom"));
    }
    Map<Path, byte[]> once = new LinkedHashMap<>(); for (Path file : files) once.put(file, Files.readAllBytes(file));
    WorldRuleFiles.beforeOpen(game, "Example");
    for (Path file : files) assertArrayEquals(once.get(file), Files.readAllBytes(file));
  }
  @Test void missingIdentityGetsOneDurableCanonicalIdentity() throws Exception {
    var level = save("level.dat", 3, null); var old = save("level.dat_old", 3, null);
    WorldRuleFiles.beforeOpen(game, "Example");
    String id = WorldRuleFiles.read(level).getCompound("Data").getString("PowerBetaCommandWorldId");
    assertEquals(UUID.fromString(id).toString(), id);
    assertEquals(id, WorldRuleFiles.read(old).getCompound("Data").getString("PowerBetaCommandWorldId"));
  }
  @Test void futureFallbackRejectsBeforeAnyCurrentMetadataWrite() throws Exception {
    var level = save("level.dat", 3, null); var old = save("level.dat_old", RuleSet.VERSION + 1, null);
    byte[] a = Files.readAllBytes(level), b = Files.readAllBytes(old);
    assertThrows(IllegalArgumentException.class, () -> WorldRuleFiles.beforeOpen(game, "Example"));
    assertArrayEquals(a, Files.readAllBytes(level)); assertArrayEquals(b, Files.readAllBytes(old));
  }
  @Test void conflictingIdentitiesAreNeverSilentlyReassigned() throws Exception {
    var level = save("level.dat", 3, UUID.randomUUID().toString());
    var old = save("level.dat_old", 3, UUID.randomUUID().toString());
    byte[] a = Files.readAllBytes(level), b = Files.readAllBytes(old);
    assertThrows(IOException.class, () -> WorldRuleFiles.beforeOpen(game, "Example"));
    assertArrayEquals(a, Files.readAllBytes(level)); assertArrayEquals(b, Files.readAllBytes(old));
  }
  @Test void futureOffhandBlocksBothMetadataWrites() throws Exception {
    var level = save("level.dat", 3, null); var old = save("level.dat_old", 3, null);
    var root = WorldRuleFiles.read(old); var player = new NbtCompound(); var hand = new NbtCompound();
    hand.putInt("format", 2); player.put("PowerBetaOffhand", hand); root.getCompound("Data").put("Player", player); write(old, root);
    byte[] a = Files.readAllBytes(level), b = Files.readAllBytes(old);
    assertThrows(IOException.class, () -> WorldRuleFiles.beforeOpen(game, "Example"));
    assertArrayEquals(a, Files.readAllBytes(level)); assertArrayEquals(b, Files.readAllBytes(old));
  }
  @Test void separateFuturePlayerDataAlsoBlocksUpgrade() throws Exception {
    var level = save("level.dat", 3, null); byte[] original = Files.readAllBytes(level);
    Path playerFile = level.getParent().resolve("players/Test.dat"); Files.createDirectories(playerFile.getParent());
    var player = new NbtCompound(); var hand = new NbtCompound(); hand.putInt("format", 2); player.put("PowerBetaOffhand", hand); write(playerFile, player);
    assertThrows(IOException.class, () -> WorldRuleFiles.beforeOpen(game, "Example"));
    assertArrayEquals(original, Files.readAllBytes(level));
  }
  @Test void mixedOffhandAndOldFallbackNeedsUpgradeBeforeAnOfflineEdit() throws Exception {
    String id = UUID.randomUUID().toString();
    var level = save("level.dat", 4, id); var old = save("level.dat_old", 3, id);
    var root = WorldRuleFiles.read(level); var player = new NbtCompound(); var hand = new NbtCompound();
    hand.putInt("format", 1); player.put("PowerBetaOffhand", hand); root.getCompound("Data").put("Player", player); write(level, root);
    assertThrows(IOException.class, () -> WorldRuleFiles.validateOfflineEdit(level, old));
    WorldRuleFiles.beforeOpen(game, "Example");
    assertDoesNotThrow(() -> WorldRuleFiles.validateOfflineEdit(level, old));
  }
}
