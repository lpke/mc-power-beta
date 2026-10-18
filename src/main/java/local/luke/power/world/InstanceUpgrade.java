package local.luke.power.world;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.FileTransaction;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.nbt.*;

/** Offline instance maintenance uses the same converters and durable transactions as the game. */
public final class InstanceUpgrade {
  private InstanceUpgrade() {}
  public static void main(String[] args) throws Exception {
    if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Usage: GAME_DIRECTORY [--defaults]");
    boolean defaults = args.length == 2 && args[1].equals("--defaults");
    if (args.length == 2 && !defaults) throw new IllegalArgumentException("Unknown option");
    Path game = Path.of(args[0]).toRealPath();
    assertIdle(game);
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    FileTransaction.recover(game);
    List<Path> files = new ArrayList<>(); files.add(PowerConfig.path());
    if (Files.isDirectory(game.resolve("saves"))) try (var paths = Files.walk(game.resolve("saves"))) {
      files.addAll(paths.filter(Files::isRegularFile).filter(p -> Set.of("level.dat", "level.dat_old").contains(p.getFileName().toString())).sorted().toList());
    }
    try (FileTransaction transaction = FileTransaction.begin(game, files)) {
      JsonObject converted = PowerConfig.document();
      Map<Path, byte[]> planned = new LinkedHashMap<>();
      Map<Path, RuleSet> expected = new LinkedHashMap<>();
      for (Path path : files.subList(1, files.size())) {
        NbtCompound root = WorldRuleFiles.read(path);
        RuleSet rules = WorldRuleData.read(root.getCompound("Data"));
        WorldRuleData.write(root.getCompound("Data"), rules);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); NbtIo.writeCompressed(root, out);
        planned.put(path, out.toByteArray()); expected.put(path, rules);
      }
      if (defaults) {
        JsonObject fresh;
        try (var in = InstanceUpgrade.class.getResourceAsStream("/assets/powerbeta/defaults/power-beta.json")) {
          if (in == null) throw new IOException("Missing pack defaults");
          fresh = JsonParser.parseReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject previous = converted.getAsJsonObject("settings").getAsJsonObject("native");
        JsonObject nativeDefaults = fresh.getAsJsonObject("settings").getAsJsonObject("native");
        for (String key : List.of("fov", "guiScale", "gui_scale", "fpsLimit", "framerate_limit"))
          if (previous != null && previous.has(key)) nativeDefaults.add(key, previous.get(key).deepCopy());
        fresh.getAsJsonObject("settings").getAsJsonObject("defaults").addProperty("setupComplete", true);
        // Keep immutable conversion evidence for worlds imported after this maintenance pass.
        for (String key : List.of("legacyWorldRules", "worldRuleUpgrade", "worldRuleUpgrade210"))
          if (converted.has(key)) fresh.add(key, converted.get(key).deepCopy());
        assertIdle(game); PowerConfig.write(fresh);
      }
      for (var entry : planned.entrySet()) {
        assertIdle(game); FileTransaction.atomicWrite(entry.getKey(), entry.getValue());
        if (!Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey()))
            || !WorldRuleData.read(WorldRuleFiles.read(entry.getKey()).getCompound("Data")).equals(expected.get(entry.getKey())))
          throw new IOException("World metadata verification failed: " + entry.getKey().getFileName());
      }
      assertIdle(game); transaction.commit();
      System.out.println("Verified configuration schema " + local.luke.power.storage.ConfigVersions.CURRENT
          + ", world-rule format " + RuleSet.VERSION + ", metadata files " + planned.size()
          + ", preferences " + (defaults ? "pack defaults with retained FOV, GUI scale and frame limit" : "preserved"));
    }
  }
  private static void assertIdle(Path game) throws IOException {
    try (var processes = Files.list(Path.of("/proc"))) {
      for (Path process : processes.filter(p -> p.getFileName().toString().matches("[0-9]+")).toList()) {
        try {
          if (Long.parseLong(process.getFileName().toString()) == ProcessHandle.current().pid()) continue;
          if (Files.readString(process.resolve("comm")).strip().equals("java")
              && process.resolve("cwd").toRealPath().equals(game))
            throw new IllegalStateException("Instance is running; maintenance stopped.");
        } catch (NoSuchFileException | AccessDeniedException ignored) { }
      }
    }
  }
}
