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
      if (PowerConfig.problem().isEmpty()) initializeBoatPlacement(game);
    }
  }

  private static void initializeBoatPlacement(Path game) throws IOException {
    JsonObject gameplay = PowerConfig.section("gameplay");
    if (gameplay.has("boatPlacementFacing")) return;
    // Freeze the first value so later reset-base changes cannot change it on restart.
    gameplay.add("boatPlacementFacing", DefaultBases.value("gameplay.boatPlacementFacing", new JsonPrimitive(true)));
    try (FileTransaction transaction = FileTransaction.begin(game, java.util.List.of(PowerConfig.path()))) {
      PowerConfig.put("gameplay", gameplay);
      transaction.commit();
    }
  }
}
