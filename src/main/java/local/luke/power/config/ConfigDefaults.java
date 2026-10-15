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
        try { PowerConfig.document(); }
        catch (IllegalStateException failure) { PowerConfig.recovery(defaults, failure.getMessage()); }
      } else PowerConfig.write(defaults);
    }
  }
}
