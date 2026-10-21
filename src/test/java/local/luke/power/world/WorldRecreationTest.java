package local.luke.power.world;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorldRecreationTest {
  @TempDir Path game;
  private Path previousConfig;
  @BeforeEach void configure() throws Exception {
    PowerConfig.flushPending(); var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true);
    previousConfig = (Path)field.get(null); field.set(null, null); PowerConfig.configure(game.resolve("config/power-beta.json"));
  }
  @AfterEach void restoreConfig() throws Exception {
    PowerConfig.flushPending(); var field = PowerConfig.class.getDeclaredField("file"); field.setAccessible(true); field.set(null, previousConfig);
  }
  @Test void recreationCopiesOnlySeedRulesDifficultyAndNameWithoutEditingSource() throws Exception {
    Path world = game.resolve("saves/World"); Files.createDirectories(world);
    NbtCompound root = new NbtCompound(), data = new NbtCompound(), player = new NbtCompound();
    data.putString("LevelName", "Seed world"); data.putLong("RandomSeed", Long.MIN_VALUE); data.putInt(WorldDifficulty.TAG, 1);
    player.putString("Inventory", "must not enter draft"); data.put("Player", player);
    RuleSet rules = new RuleSet(); rules.set(GameRule.FIRE_SPREAD, false); WorldRuleData.write(data, rules); root.put("Data", data);
    Path level = world.resolve("level.dat"); try (OutputStream output = Files.newOutputStream(level)) { NbtIo.writeCompressed(root, output); }
    byte[] original = Files.readAllBytes(level);
    var draft = WorldRecreation.read(game, "World", () -> true);
    assertEquals("Seed world", draft.name()); assertEquals(Long.MIN_VALUE, draft.seed()); assertEquals(1, draft.difficulty());
    assertFalse(draft.rules().enabled(GameRule.FIRE_SPREAD));
    RuleSet changed = draft.rules(); changed.set(GameRule.FIRE_SPREAD, true); assertFalse(draft.rules().enabled(GameRule.FIRE_SPREAD));
    assertArrayEquals(original, Files.readAllBytes(level)); assertFalse(Files.exists(world.resolve("level.dat_old")));
  }
  @Test void exactNumericSeedsIncludeZeroAndFullLongRangeWhileTextKeepsNativeHashing() {
    assertEquals(0, WorldRecreation.exactSeed("0", 123));
    assertEquals(Long.MIN_VALUE, WorldRecreation.exactSeed(Long.toString(Long.MIN_VALUE), 123));
    assertEquals(Long.MAX_VALUE, WorldRecreation.exactSeed(Long.toString(Long.MAX_VALUE), 123));
    for (String value : new String[]{"", "text seed", "9223372036854775808"}) assertEquals(123, WorldRecreation.exactSeed(value, 123));
  }
}
