package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicPresetMigrationTest {
  @TempDir Path game;

  @Test void legacyPoolsFreezeOnceAndKeepBothContextsAndMissingExclusions() throws Exception {
    byte[] ogg = {'O','g','g','S',0,0,0,0,0,0,0,0};
    for (String folder : List.of("world", "menu")) {
      Files.createDirectory(game.resolve(folder));
      Files.write(game.resolve(folder + "/one.ogg"), ogg);
    }
    var document = JsonParser.parseString("""
      {"settings":{"audio":{"musicDirectories":["world"],"menuDirectories":["menu"],
       "preset":"saved","presets":[{"id":"saved","name":"Saved", "excluded":["music:missing.ogg"]}]}}}
      """).getAsJsonObject();
    MusicPresetMigration.upgrade(document, game);
    var audio = new Gson().fromJson(document.getAsJsonObject("settings").get("audio"), AudioSettings.class);
    audio.validate();
    var preset = audio.presets.get(0);
    assertEquals(BuiltinMusic.TRACKS.size() + 3, preset.trackPool().size());
    assertEquals(preset.trackPool(), audio.presetTrackPool);
    assertFalse(preset.includes("music:missing.ogg"));
    Files.write(game.resolve("world/two.ogg"), ogg);
    String snapshot = document.toString();
    MusicPresetMigration.upgrade(document, game);
    assertEquals(snapshot, document.toString());
    String added = MusicLibrary.scan(game, List.of("world"), false).tracks().stream()
        .filter(t -> t.name().equals("two.ogg")).findFirst().orElseThrow().id();
    assertFalse(preset.includes(added));
  }
}
