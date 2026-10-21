package local.luke.power.world;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import local.luke.power.rules.RuleSet;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

/** Creation inputs only. No player, terrain, world ID or recovery data enters the new world. */
public final class WorldRecreation {
  public record Draft(String name, long seed, int difficulty, RuleSet rules) {
    public Draft { rules = rules.copy(); }
    @Override public RuleSet rules() { return rules.copy(); }
  }
  private static final Map<Screen, Draft> DRAFTS = new WeakHashMap<>();
  private WorldRecreation() {}

  public static Draft read(Path game, String folder, BooleanSupplier unloaded) throws IOException {
    SavedWorldAccess access = new SavedWorldAccess(game, folder, unloaded);
    try (var lease = access.acquire()) {
      WorldRuleFiles.validateOfflineEdit(access.level(), access.old());
      byte[] level = SavedWorldAccess.metadataBytes(access.level()), old = SavedWorldAccess.metadataBytes(access.old());
      Path source = level != null ? access.level() : access.old();
      if (level == null && old == null) throw new IOException("World metadata is missing. Restore a verified backup first.");
      NbtCompound data = WorldRuleFiles.read(source).getCompound("Data");
      if (!data.contains("RandomSeed")) throw new IOException("This world's seed is missing.");
      int difficulty = data.contains(WorldDifficulty.TAG) ? WorldDifficulty.checked(data.getInt(WorldDifficulty.TAG)) : WorldDifficulty.DEFAULT;
      String name = data.getString("LevelName").isBlank() ? folder : data.getString("LevelName");
      Draft draft = new Draft(name, data.getLong("RandomSeed"), difficulty, WorldRuleData.read(data));
      access.checkIdle(lease);
      if (!Arrays.equals(level, SavedWorldAccess.metadataBytes(access.level())) || !Arrays.equals(old, SavedWorldAccess.metadataBytes(access.old())))
        throw new IOException("World metadata changed. Reopen the world menu and retry.");
      return draft;
    }
  }
  public static synchronized void prepare(Screen screen, Draft draft) { DRAFTS.put(Objects.requireNonNull(screen), Objects.requireNonNull(draft)); }
  public static synchronized Draft take(Screen screen) { return DRAFTS.remove(screen); }

  /** Native Beta treats numeric zero as random. A recreated seed must retain zero too. */
  public static long exactSeed(String entered, long nativeSeed) {
    if (entered == null || entered.isBlank()) return nativeSeed;
    try { return Long.parseLong(entered); }
    catch (NumberFormatException textSeed) { return nativeSeed; }
  }
}
