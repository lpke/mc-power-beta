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
    if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Usage: GAME_DIRECTORY [--defaults|--rule-defaults|--sorting-layouts]");
    boolean templates = args.length == 2 && args[1].equals("--rule-defaults");
    boolean defaults = args.length == 2 && args[1].equals("--defaults");
    boolean sorting = args.length == 2 && args[1].equals("--sorting-layouts");
    if (args.length == 2 && !defaults && !templates && !sorting) throw new IllegalArgumentException("Unknown option");
    Path game = Path.of(args[0]).toRealPath();
    assertIdle(game);
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    FileTransaction.recover(game);
    if (templates) {
      local.luke.power.config.DefaultBases.reload();
      JsonObject before = PowerConfig.document();
      assertIdle(game); local.luke.power.config.RuleDefaults.resetLatest(); assertIdle(game);
      JsonObject after = PowerConfig.document();
      before.getAsJsonObject("settings").remove("gameRules"); after.getAsJsonObject("settings").remove("gameRules");
      if (!before.equals(after)) throw new IOException("Creation-default maintenance changed unrelated settings.");
      System.out.println("Updated only Survival and Cheats creation templates for " + local.luke.power.config.DefaultBases.current().label + ". Verified backup retained; worlds untouched.");
      return;
    }
    List<Path> worlds = new ArrayList<>();
    if (Files.isDirectory(game.resolve("saves"))) try (var paths = Files.list(game.resolve("saves"))) {
      worlds.addAll(paths.filter(Files::isDirectory).sorted().toList());
    }
    if (sorting) {
      migrateSortingLayouts(game, worlds); return;
    }
    var originals = new LinkedHashMap<Path, byte[]>();
    var planned = new LinkedHashMap<Path, byte[]>();
    // Reject unsupported data in every world before publishing any converted metadata.
    for (Path world : worlds) {
      SavedWorldAccess.requireOpenable(game, world.getFileName().toString());
      OwnedMetadataTransaction.recover(game, world);
      var upgrade = WorldRuleFiles.planUpgrade(world);
      originals.putAll(upgrade.originals()); planned.putAll(upgrade.planned());
    }
    try (FileTransaction transaction = FileTransaction.begin(game, List.of(PowerConfig.path()));
        OwnedMetadataTransaction metadata = planned.isEmpty() ? null : OwnedMetadataTransaction.begin(game, originals, planned)) {
      JsonObject converted = PowerConfig.document();
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
        // Saved libraries and hotbars are user data, independent of resettable preferences.
        JsonObject oldSettings = converted.getAsJsonObject("settings"), freshSettings = fresh.getAsJsonObject("settings");
        preserveSavedData(oldSettings, freshSettings);
        // Keep immutable conversion evidence for worlds imported after this maintenance pass.
        for (String key : List.of("legacyWorldRules", "worldRuleUpgrade", "worldRuleUpgrade210"))
          if (converted.has(key)) fresh.add(key, converted.get(key).deepCopy());
        assertIdle(game); PowerConfig.write(fresh);
      }
      assertIdle(game);
      if (metadata != null) metadata.apply();
      for (var entry : originals.entrySet()) {
        byte[] expected = planned.getOrDefault(entry.getKey(), entry.getValue());
        if (!Arrays.equals(expected, Files.readAllBytes(entry.getKey())))
          throw new IOException("World metadata changed during maintenance.");
      }
      assertIdle(game);
      migrateSortingLayouts(game, worlds);
      if (metadata != null) metadata.commit();
      transaction.commit();
      System.out.println("Verified configuration schema " + local.luke.power.storage.ConfigVersions.CURRENT
          + ", world-rule format " + RuleSet.VERSION + ", metadata files " + planned.size()
          + ", preferences " + (defaults ? "pack defaults with retained FOV, GUI scale and frame limit" : "preserved"));
    }
  }
  static void preserveSavedData(JsonObject oldSettings, JsonObject freshSettings) {
    // Keep opaque unsupported formats too. A defaults reset never owns user libraries.
    for (String section : List.of("savedCreativeHotbars", "sortingLayouts", "worldSortingLayouts"))
      if (oldSettings.has(section)) freshSettings.add(section, oldSettings.get(section).deepCopy());
    if (!oldSettings.has("worldSortingLayouts") && (oldSettings.has("sortingLayouts") || oldSettings.has("inventoryTools")))
      freshSettings.add("worldSortingLayouts", local.luke.power.inventory.tools.WorldSortingLayoutsData.migrate(oldSettings).json());
    for (String[] field : new String[][]{{"audio", "presets"}, {"audio", "favourites"}, {"logo", "presets"}, {"interface", "debugPresets"}}) {
      if (oldSettings.has(field[0]) && oldSettings.getAsJsonObject(field[0]).has(field[1]))
        freshSettings.getAsJsonObject(field[0]).add(field[1], oldSettings.getAsJsonObject(field[0]).get(field[1]).deepCopy());
    }
  }
  private static void migrateSortingLayouts(Path game, List<Path> worlds) throws IOException {
    JsonObject settings = PowerConfig.document().getAsJsonObject("settings");
    JsonElement original = settings.get(local.luke.power.inventory.tools.WorldSortingLayoutsData.SECTION);
    var layouts = local.luke.power.inventory.tools.WorldSortingLayoutsData.migrate(settings);
    var ids = new LinkedHashSet<String>(); var evidence = new LinkedHashMap<Path, byte[]>();
    for (Path world : worlds) {
      SavedWorldAccess.safe(world);
      String identity = null;
      try (var files = Files.walk(world, 32)) {
        for (Path path : files.filter(p -> Set.of("level.dat", "level.dat_old").contains(p.getFileName().toString())).sorted().toList()) {
          SavedWorldAccess.safe(path);
          if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("World metadata is not a regular file.");
          byte[] bytes = Files.readAllBytes(path); evidence.put(path, bytes);
          String id = WorldRuleFiles.read(path).getCompound("Data").getString("PowerBetaCommandWorldId");
          try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
          catch (IllegalArgumentException invalid) { throw new IOException("Sorting migration requires a stable world ID. Run normal instance maintenance first.", invalid); }
          if (identity != null && !identity.equals(id)) throw new IOException("Conflicting world identities; sorting layouts were not changed.");
          identity = id;
        }
      }
      if (identity != null) {
        ids.add(identity);
        if (!layouts.contains(identity)) {
          JsonObject imported = local.luke.power.inventory.tools.WorldSortingLayoutCopies.imported(world, identity);
          if (imported != null) layouts = layouts.replace(identity, imported);
        }
      }
    }
    var next = layouts.seed(ids);
    for (String id : ids) next.layout(id);
    assertIdle(game);
    for (var entry : evidence.entrySet()) if (!Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())))
      throw new IOException("World metadata changed during sorting migration.");
    if (!Objects.equals(original, next.json())) PowerConfig.replaceSection(local.luke.power.inventory.tools.WorldSortingLayoutsData.SECTION, original, next.json());
    assertIdle(game);
    for (var entry : evidence.entrySet()) if (!Arrays.equals(entry.getValue(), Files.readAllBytes(entry.getKey())))
      throw new IOException("World metadata changed during sorting migration; restore the verified settings backup.");
    System.out.println("Verified per-world sorting layouts for " + ids.size() + " worlds; retained global layouts and all world metadata bytes.");
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
