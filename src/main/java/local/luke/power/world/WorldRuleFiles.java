package local.luke.power.world;

import com.google.gson.*;
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
    SavedWorldAccess.requireOpenable(game, folder);
    Path saves = game.resolve("saves").toAbsolutePath().normalize();
    Path world = saves.resolve(folder).normalize();
    if (!world.startsWith(saves) || world.equals(saves)) throw new IOException("Invalid world folder.");
    checkPath(world);
    if (!Files.exists(world)) return;
    try (var lease = new SavedWorldAccess(game, folder, () -> true).acquire()) {
      fenceExisting(game, world);
    }
  }

  private static void fenceExisting(Path game, Path world) throws IOException {
    OwnedMetadataTransaction.recover(game, world);
    Upgrade upgrade = planUpgrade(world);
    if (upgrade.planned().isEmpty()) return;
    try (OwnedMetadataTransaction transaction = OwnedMetadataTransaction.begin(game, upgrade.originals(), upgrade.planned())) {
      unchanged(upgrade.originals()); transaction.apply();
      for (var entry : upgrade.originals().entrySet()) {
        byte[] expected = upgrade.planned().getOrDefault(entry.getKey(), entry.getValue());
        if (!Arrays.equals(Files.readAllBytes(entry.getKey()), expected))
          throw new IOException("World metadata changed during upgrade.");
      }
      transaction.commit();
    }
  }

  public record Upgrade(Map<Path, byte[]> originals, Map<Path, byte[]> planned) {}

  /** Validate every input before the first write, also used by offline instance maintenance. */
  public static Upgrade planUpgrade(Path world) throws IOException {
    var originals = new LinkedHashMap<Path, byte[]>();
    var planned = new LinkedHashMap<Path, byte[]>();
    var roots = new LinkedHashMap<Path, NbtCompound>();
    var rules = new LinkedHashMap<Path, RuleSet>();
    String identity = null;
    try (var paths = Files.walk(world, 32)) {
      for (Path path : paths.filter(p -> Set.of("level.dat", "level.dat_old").contains(p.getFileName().toString())).sorted().toList()) {
        checkPath(path);
        if (!Files.isRegularFile(path)) throw new IOException("World metadata is not a regular file.");
        if (originals.size() >= 256) throw new IOException("Too many world metadata files to upgrade safely.");
        if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("World metadata exceeds 4 MiB.");
        byte[] original = Files.readAllBytes(path);
        NbtCompound root = readBytes(original), data = root.getCompound("Data");
        rules.put(path, WorldRuleData.read(data)); // Resolve frozen legacy evidence before assigning an ID.
        local.luke.power.offhand.OffhandData.validateWorldData(data);
        originals.put(path, original);
        roots.put(path, root);
        if (data.contains("PowerBetaCommandWorldId")) {
          String id = data.getString("PowerBetaCommandWorldId");
          try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
          catch (IllegalArgumentException invalid) { throw new IOException("Invalid stable world identity. Restore a verified metadata backup.", invalid); }
          if (identity != null && !identity.equals(id)) throw new IOException("World metadata has conflicting identities. Restore matching level.dat and level.dat_old backups.");
          identity = id;
        }
      }
    }
    validatePlayers(world);
    if (identity == null) identity = UUID.randomUUID().toString();
    for (var entry : roots.entrySet()) {
      NbtCompound data = entry.getValue().getCompound("Data");
      if (!data.contains(WorldRuleData.TAG) || data.getCompound(WorldRuleData.TAG).getInt("version") != RuleSet.VERSION
          || !identity.equals(data.getString("PowerBetaCommandWorldId"))) {
        WorldRuleData.write(data, rules.get(entry.getKey())); data.putString("PowerBetaCommandWorldId", identity);
        planned.put(entry.getKey(), fencedMetadata(entry.getValue()));
      }
    }
    return new Upgrade(originals, planned);
  }

  /** Viewing/renaming may validate a newer inventory without modifying the retained previous metadata. */
  public static void validateOfflineEdit(Path level, Path old) throws IOException {
    boolean newInventory = false, oldFormat = false;
    for (Path path : List.of(level, old)) if (Files.exists(path)) {
      checkPath(path); NbtCompound data = read(path).getCompound("Data");
      WorldRuleData.read(data); local.luke.power.offhand.OffhandData.validateWorldData(data);
      oldFormat |= !data.contains(WorldRuleData.TAG) || data.getCompound(WorldRuleData.TAG).getInt("version") < 4;
      NbtCompound player = data.getCompound("Player");
      newInventory |= containsNewInventory(player);
    }
    Path world = level.getParent(); validatePlayers(world);
    Path players = world.resolve("players");
    if (Files.isDirectory(players)) try (var paths = Files.list(players)) {
      for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".dat")).toList())
        try (InputStream input = Files.newInputStream(path)) { newInventory |= containsNewInventory(NbtIo.readCompressed(input)); }
    }
    if (newInventory && oldFormat)
      throw new IOException("This world's new inventory data has an older metadata fallback. Run instance maintenance or open it with Power Beta 4.0.0 to verify and upgrade both files first.");
  }

  private static boolean containsNewInventory(NbtCompound player) throws IOException {
    if (player.contains(local.luke.power.offhand.OffhandData.TAG)) return true;
    if (!player.contains("PowerBetaKeptInventory")) return false;
    byte[] bytes = player.getByteArray("PowerBetaKeptInventory");
    if (bytes.length == 0) return false; // Ordinary historical saves include this empty tag.
    local.luke.power.death.KeptInventory.read(bytes);
    JsonObject kept;
    try { kept = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject(); }
    catch (RuntimeException error) { throw new IOException("Invalid kept inventory data", error); }
    return kept.has("offhand") && !kept.get("offhand").isJsonNull();
  }

  /** Shared by staged world copies. Changes only the validated rule format and preserves every other tag. */
  public static byte[] fencedMetadata(NbtCompound root) throws IOException {
    if (root == null || !root.contains("Data")) throw new IOException("World metadata has no Data compound.");
    NbtCompound data = root.getCompound("Data");
    RuleSet expected = WorldRuleData.read(data);
    local.luke.power.offhand.OffhandData.validateWorldData(data);
    WorldRuleData.write(data, expected);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.writeCompressed(root, bytes);
    if (bytes.size() > 4 * 1024 * 1024) throw new IOException("Converted metadata exceeds 4 MiB.");
    NbtCompound check = NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
    if (!WorldRuleData.read(check.getCompound("Data")).equals(expected))
      throw new IOException("Converted world rules could not be verified.");
    return bytes.toByteArray();
  }

  private static void unchanged(Map<Path, byte[]> originals) throws IOException {
    for (var entry : originals.entrySet()) {
      checkPath(entry.getKey());
      if (!Arrays.equals(Files.readAllBytes(entry.getKey()), entry.getValue()))
        throw new IOException("World metadata changed. Reopen the world before upgrading.");
    }
  }

  public static void validatePlayers(Path world) throws IOException {
    Path players = world.resolve("players");
    if (!Files.exists(players)) return;
    checkPath(players);
    try (var paths = Files.list(players)) {
      var files = paths.filter(p -> p.getFileName().toString().endsWith(".dat")).limit(4097).toList();
      if (files.size() > 4096) throw new IOException("Too many saved player files to validate safely.");
      for (Path path : files) {
        checkPath(path);
        if (!Files.isRegularFile(path) || Files.size(path) > 4 * 1024 * 1024)
          throw new IOException("Invalid saved player data.");
        try (InputStream input = Files.newInputStream(path)) {
          local.luke.power.offhand.OffhandData.validatePlayer(NbtIo.readCompressed(input));
        }
      }
    }
  }
  public static NbtCompound read(Path path) throws IOException {
    if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("World metadata exceeds 4 MiB: " + path.getFileName());
    return readBytes(Files.readAllBytes(path));
  }
  private static NbtCompound readBytes(byte[] bytes) throws IOException {
    if (bytes.length > 4 * 1024 * 1024) throw new IOException("World metadata exceeds 4 MiB.");
    try (InputStream input = new ByteArrayInputStream(bytes)) {
      NbtCompound root = NbtIo.readCompressed(input);
      if (root == null || !root.contains("Data")) throw new IOException("World metadata has no Data compound.");
      return root;
    } catch (RuntimeException e) { throw new IOException("Invalid world metadata: " + e.getMessage(), e); }
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
