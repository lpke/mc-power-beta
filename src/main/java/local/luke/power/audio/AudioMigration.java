package local.luke.power.audio;

import com.google.gson.*;
import java.util.List;

/** Idempotent conversion; the caller backs up the complete document before writing. */
public final class AudioMigration {
  private AudioMigration() {}

  public static void upgrade(JsonObject document) {
    if (document.has("musicSettingsVersion")) return;
    JsonObject settings = document.getAsJsonObject("settings");
    if (settings == null) throw new IllegalArgumentException("Missing settings document");
    JsonObject audio = section(settings, "audio");
    JsonObject environment = settings.getAsJsonObject("power_environment:config");
    JsonObject old = environment == null ? null : environment.getAsJsonObject("MUSIC_CONFIG");
    if (old == null) old = new JsonObject();
    String mode = audio.has("musicMode") ? audio.get("musicMode").getAsString() : "VANILLA";
    if (mode.equals("ADD") || mode.equals("REPLACE")) {
      audio.addProperty("musicMode", "VANILLA");
      audio.addProperty("customMusic", mode.equals("ADD") ? "ADD" : "ONLY");
    }
    if (bool(old, "disableDefaultMinecraftBGM")) audio.addProperty("customMusic", "ONLY");
    if (bool(old, "disableBackgroundMusic")) section(settings, "native").addProperty("music", "0.0");
    if (!audio.has("menuMusic")) audio.addProperty("menuMusic",
        bool(old, "mainMenuThemeEnabled") ? "CUSTOM" : "WORLD");
    if (!audio.has("dimensionMusic")) audio.addProperty("dimensionMusic",
        bool(old, "stopCurrentBgmOnPortalUse") ? "STOP_ALL"
            : bool(old, "stopDimensionSpecificSongOnPortalUse") ? "STOP_SPECIFIC" : "CONTINUE");
    long min = Math.max(0, Math.min(1728000, number(old, "musicCoundownRandomIntervalMin", 12000)));
    long extra = Math.max(1, Math.min(1728001, number(old, "musicCoundownRandomIntervalMax", 12000)));
    // The old "max" was an exclusive random ADDITION, not a maximum gap.
    if (!audio.has("gapMinSeconds")) audio.addProperty("gapMinSeconds", seconds(min));
    if (!audio.has("gapMaxSeconds")) audio.addProperty("gapMaxSeconds", seconds(min + extra - 1));
    for (String key : List.of("disableDefaultMinecraftBGM", "disableBackgroundMusic",
        "mainMenuThemeEnabled", "mainMenuThemeOverridesBGM", "stopCurrentBgmOnPortalUse",
        "stopDimensionSpecificSongOnPortalUse", "musicCoundownRandomIntervalMin", "musicCoundownRandomIntervalMax"))
      old.remove(key);
    document.addProperty("musicSettingsVersion", 1);
  }

  private static int seconds(long ticks) { return (int) Math.min(86400, (ticks + 19) / 20); }
  private static boolean bool(JsonObject o, String key) { return o.has(key) && o.get(key).getAsBoolean(); }
  private static long number(JsonObject o, String key, long fallback) { return o.has(key) ? o.get(key).getAsLong() : fallback; }
  private static JsonObject section(JsonObject o, String key) {
    if (!o.has(key)) o.add(key, new JsonObject());
    return o.getAsJsonObject(key);
  }
}
