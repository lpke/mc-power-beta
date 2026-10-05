package local.luke.power.audio;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;

/** Freeze legacy presets once, before folder edits can introduce new implicit inclusions. */
public final class MusicPresetMigration {
  private MusicPresetMigration() {}

  public static void upgrade(JsonObject document, Path game) {
    if (document.has("musicPresetVersion")) return;
    JsonObject audio = document.getAsJsonObject("settings").getAsJsonObject("audio");
    if (audio != null && audio.has("presets")) {
      Set<String> available = new TreeSet<>();
      BuiltinMusic.TRACKS.forEach(t -> available.add(t.id()));
      boolean recursive = audio.has("recursive") && audio.get("recursive").getAsBoolean();
      for (String key : List.of("musicDirectories", "menuDirectories"))
        if (audio.has(key)) {
          List<String> folders = new ArrayList<>();
          audio.getAsJsonArray(key).forEach(v -> folders.add(v.getAsString()));
          MusicLibrary.scan(game, folders, recursive).tracks().forEach(t -> available.add(t.id()));
        }
      String selected = audio.has("preset") ? audio.get("preset").getAsString() : "";
      for (JsonElement value : audio.getAsJsonArray("presets")) {
        JsonObject preset = value.getAsJsonObject();
        if (!preset.has("trackPool") || preset.get("trackPool").isJsonNull()) {
          Set<String> pool = new TreeSet<>(available);
          if (preset.has("excluded")) preset.getAsJsonArray("excluded").forEach(v -> pool.add(v.getAsString()));
          JsonArray array = new JsonArray();
          pool.forEach(array::add);
          preset.add("trackPool", array);
        }
        if (preset.get("id").getAsString().equals(selected) && !audio.has("presetTrackPool"))
          audio.add("presetTrackPool", preset.get("trackPool").deepCopy());
      }
    }
    document.addProperty("musicPresetVersion", 1);
  }
}
