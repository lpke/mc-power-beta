package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import local.luke.power.storage.PowerConfig;

/** Install the current pack defaults once, before modules read their settings. */
public final class ConfigDefaults {
  private ConfigDefaults() {}

  public static void prepare(Path game) throws IOException {
    PowerConfig.configure(game.resolve("config/power-beta.json"));
    try (var input = ConfigDefaults.class.getResourceAsStream("/assets/powerbeta/defaults/power-beta.json")) {
      if (input == null) throw new IOException("Missing Power Beta defaults");
      JsonObject defaults = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
      if (Files.exists(PowerConfig.path(), LinkOption.NOFOLLOW_LINKS)) {
        try {
          JsonObject document = PowerConfig.document();
          if (!document.getAsJsonObject("settings").has("defaults")) {
            DefaultBases.State state = new DefaultBases.State(); state.setupComplete = true;
            try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
              PowerConfig.save("defaults", state); transaction.commit();
            }
          }
        }
        catch (IllegalStateException failure) { PowerConfig.recovery(defaults, failure.getMessage()); }
      } else PowerConfig.write(defaults);
      try { DefaultBases.reload(); }
      catch (RuntimeException failure) {
        PowerConfig.recovery(defaults, failure.getMessage()); DefaultBases.reload();
      }
      if (PowerConfig.problem().isEmpty()) {
        initializeGameplay(game);
        initializeMaps(game);
        initializeExtraSounds(game);
        initializeInterface(game);
        initializeModernFeatures(game);
      }
    }
  }

  /** Freeze new feature values once against the selected base, with one verified backup. */
  private static void initializeModernFeatures(Path game) throws IOException {
    java.util.Map<String, JsonObject> changed = new java.util.LinkedHashMap<>();
    for (String id : java.util.List.of("offhand.enabled", "recipeBook.enabled", "subtitles.enabled", "heldLight.enabled", "inventoryTools.sorting", "inventoryTools.depositMatching", "inventoryTools.creativeHotbars", "inventoryTools.restock", "inventoryTools.toolSwitch", "worldTools.backup", "worldTools.duplicate", "worldTools.recreate")) {
      int split = id.indexOf('.'); String section = id.substring(0, split), key = id.substring(split + 1);
      JsonObject values = changed.computeIfAbsent(section, PowerConfig::section);
      if (!values.has(key)) values.add(key, DefaultBases.value(id, new JsonPrimitive(false)));
    }
    changed.entrySet().removeIf(e -> e.getValue().equals(PowerConfig.section(e.getKey())));
    if (changed.isEmpty()) return;
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      for (var entry : changed.entrySet()) PowerConfig.put(entry.getKey(), entry.getValue());
      transaction.commit();
    }
  }

  private static void initializeMaps(Path game) throws IOException {
    JsonObject maps = PowerConfig.section("maps");
    if (maps.has("minimap")) return;
    maps.add("minimap", DefaultBases.value("maps.minimap", new JsonPrimitive(true)));
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      PowerConfig.put("maps", maps);
      transaction.commit();
    }
  }

  private static void initializeExtraSounds(Path game) throws IOException {
    JsonObject audio = PowerConfig.section("audio");
    boolean changed = false;
    for (String key : java.util.List.of("seedPlantingSounds", "hoeTillingSounds", "bucketSounds", "boneMealSounds")) {
      if (audio.has(key)) continue;
      audio.add(key, DefaultBases.value("audio." + key, new JsonPrimitive(false)));
      changed = true;
    }
    if (!changed) return;
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      PowerConfig.put("audio", audio);
      transaction.commit();
    }
  }

  private static void initializeInterface(Path game) throws IOException {
    JsonObject menu = PowerConfig.section("interface");
    boolean changed = false;
    for (String key : java.util.List.of("pauseMapButton", "pauseWaypointsButton", "debugAxes")) {
      if (menu.has(key)) continue;
      menu.add(key, DefaultBases.value("interface." + key, new JsonPrimitive(true)));
      changed = true;
    }
    if (!changed) return;
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      PowerConfig.put("interface", menu);
      transaction.commit();
    }
  }

  private static void initializeGameplay(Path game) throws IOException {
    JsonObject gameplay = PowerConfig.section("gameplay");
    boolean changed = false;
    // Freeze the first value so later reset-base changes cannot change it on restart.
    for (String key : java.util.List.of("boatPlacementFacing", "modernWaterMovement")) {
      if (gameplay.has(key)) continue;
      gameplay.add(key, DefaultBases.value("gameplay." + key, new JsonPrimitive(true)));
      changed = true;
    }
    if (!changed) return;
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      PowerConfig.put("gameplay", gameplay);
      transaction.commit();
    }
  }
}
