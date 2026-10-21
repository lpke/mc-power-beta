package local.luke.power.world;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.rules.RuleSet;
import net.minecraft.nbt.*;

/** Edits an unopened world's rules without constructing a world or rewriting player data. */
public final class SavedWorldRules {
  private final Path game, level, old;
  private final SavedWorldAccess access;
  private byte[] expectedLevel, expectedOld;
  private RuleSet rules;
  private String name;

  public SavedWorldRules(Path game, String folder, BooleanSupplier unloaded) throws IOException {
    access = new SavedWorldAccess(game, folder, unloaded);
    this.game = access.game();
    level = access.level(); old = access.old();
    try (var lease = access.acquire()) { OwnedMetadataTransaction.recover(game, access.world()); }
    checkIdle();
    expectedLevel = bytes(level); expectedOld = bytes(old);
    NbtCompound data = WorldRuleFiles.read(source()).getCompound("Data");
    rules = WorldRuleData.read(data);
    name = data.getString("LevelName").isBlank() ? folder : data.getString("LevelName");
  }

  public String name() { return name; }
  public RuleSet rules() { return rules.copy(); }

  public void validate() throws IOException {
    validate(null);
  }
  private void validate(SavedWorldAccess.Lease lease) throws IOException {
    access.checkIdle(lease);
    WorldRuleFiles.validateOfflineEdit(level, old);
    if (!Arrays.equals(expectedLevel, bytes(level)) || !Arrays.equals(expectedOld, bytes(old)))
      throw new IOException("World metadata changed. Reopen the world editor before saving.");
  }

  public void save(RuleSet next) throws IOException {
    next = next.copy();
    validate();
    if (rules.equals(next)) return;
    RuleSet chosen = next;
    edit(data -> WorldRuleData.write(data, chosen), data -> WorldRuleData.read(data).equals(chosen));
    rules = chosen;
  }

  public void rename(String next) throws IOException {
    String chosen = next.strip();
    if (chosen.isEmpty() || chosen.length() > 128 || chosen.chars().anyMatch(Character::isISOControl))
      throw new IOException("Use a world name of 1 to 128 characters.");
    validate();
    if (name.equals(chosen)) return;
    edit(data -> data.putString("LevelName", chosen), data -> data.getString("LevelName").equals(chosen));
    name = chosen;
  }

  private void edit(java.util.function.Consumer<NbtCompound> change, java.util.function.Predicate<NbtCompound> verified) throws IOException {
    byte[] saved;
    try (var lease = access.acquire()) {
      validate(lease);
      NbtCompound root = WorldRuleFiles.read(source());
      change.accept(root.getCompound("Data"));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      NbtIo.writeCompressed(root, output); saved = output.toByteArray();
      validate(lease);
      Map<Path, byte[]> originals = new LinkedHashMap<>(); originals.put(level, expectedLevel);
      try (var transaction = OwnedMetadataTransaction.begin(game, originals, Map.of(level, saved))) {
        transaction.apply(); access.checkIdle(lease);
        if (!Arrays.equals(saved, bytes(level)) || !Arrays.equals(expectedOld, bytes(old))
            || !verified.test(WorldRuleFiles.read(level).getCompound("Data")))
          throw new IOException("Saved world metadata could not be verified. Owned changes will be restored.");
        transaction.commit();
      }
    }
    expectedLevel = saved;
  }

  private Path source() throws IOException {
    if (Files.isRegularFile(level)) return level;
    if (Files.isRegularFile(old)) return old;
    throw new IOException("World metadata is missing. Restore level.dat from a backup.");
  }
  private byte[] bytes(Path path) throws IOException {
    return SavedWorldAccess.metadataBytes(path);
  }
  private void checkIdle() throws IOException {
    access.checkIdle();
    WorldRuleFiles.validateOfflineEdit(level, old);
  }
}
