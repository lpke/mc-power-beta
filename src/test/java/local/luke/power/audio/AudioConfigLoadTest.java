package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudioConfigLoadTest {
  @TempDir Path game;

  private static Map<Field, Object> snapshot(Class<?> owner, String... names) throws Exception {
    Map<Field, Object> values = new LinkedHashMap<>();
    for (String name : names) {
      Field field = owner.getDeclaredField(name);
      field.setAccessible(true);
      values.put(field, field.get(null));
    }
    return values;
  }

  private static void restore(Map<Field, Object> values) throws Exception {
    for (var value : values.entrySet()) value.getKey().set(null, value.getValue());
  }

  @Test void invalidPreferencesLatchSavingOffAndPreserveHealthyMusicData() throws Exception {
    PowerConfig.flushPending();
    Map<Field, Object> storage = snapshot(PowerConfig.class, "file", "recoveryDefaults", "problem");
    Map<Field, Object> audio = snapshot(AudioConfig.class, "current", "loadProblem");
    try {
      for (var value : storage.entrySet())
        value.getKey().set(null, value.getKey().getName().equals("problem") ? "" : null);
      PowerConfig.configure(game.resolve("config/power-beta.json"));
      Files.createDirectories(PowerConfig.path().getParent());
      JsonObject config = new JsonObject();
      config.addProperty("schemaVersion", 9);
      JsonObject settings = new JsonObject();
      JsonObject preferences = new JsonObject();
      preferences.addProperty("master", 100);
      settings.add("audio", preferences);
      config.add("settings", settings);
      PowerConfig.write(config);
      PowerConfig.initializeMusicData();
      AudioSettings healthy = new AudioSettings();
      healthy.playlists.add(new MusicPlaylist("saved", "Saved songs", List.of("music:missing", "music:calm1.ogg"), Map.of("Alpha", 40)));
      MusicPlaylist.load(healthy, "saved");
      healthy.musicDirectories.add("personal-music");
      healthy.favourites.add("music:calm1.ogg");
      healthy.sounds.put("music:calm1.ogg", 35);
      PowerConfig.saveAudio(healthy);
      MusicQueue queue = new MusicQueue();
      queue.add("music:missing"); queue.add("music:calm1.ogg"); queue.add("music:missing");
      PowerConfig.saveMusicQueue(queue);
      assertEquals(MusicPlaylist.world(healthy).tracks(), MusicPlaylist.world(PowerConfig.readAudio(AudioSettings.class)).tracks());

      JsonObject invalid = JsonParser.parseString(Files.readString(PowerConfig.path())).getAsJsonObject();
      invalid.getAsJsonObject("settings").getAsJsonObject("audio").addProperty("master", 101);
      Files.writeString(PowerConfig.path(), invalid.toString());
      byte[] originalConfig = Files.readAllBytes(PowerConfig.path());
      byte[] originalMusic = Files.readAllBytes(PowerConfig.musicPath());
      var load = AudioConfig.class.getDeclaredMethod("load");
      load.setAccessible(true);
      AudioSettings fallback = (AudioSettings) load.invoke(null);
      Field current = AudioConfig.class.getDeclaredField("current");
      current.setAccessible(true);
      current.set(null, fallback);
      assertTrue(fallback.playlists.isEmpty());
      assertEquals(100, fallback.master);
      IOException failure = assertThrows(IOException.class, () -> AudioConfig.save(fallback));
      assertTrue(failure.getMessage().contains("config/power-beta.json"));
      assertTrue(failure.getMessage().contains("power-beta/music.json"));
      assertTrue(failure.getMessage().contains("restart"));
      assertArrayEquals(originalConfig, Files.readAllBytes(PowerConfig.path()));
      assertArrayEquals(originalMusic, Files.readAllBytes(PowerConfig.musicPath()));

      // Repairing bytes alone cannot silently re-enable a fallback settings snapshot.
      invalid.getAsJsonObject("settings").getAsJsonObject("audio").addProperty("master", 100);
      Files.writeString(PowerConfig.path(), invalid.toString());
      byte[] repairedConfig = Files.readAllBytes(PowerConfig.path());
      assertThrows(IOException.class, () -> AudioConfig.save(fallback));
      assertArrayEquals(repairedConfig, Files.readAllBytes(PowerConfig.path()));
      assertArrayEquals(originalMusic, Files.readAllBytes(PowerConfig.musicPath()));
      AudioSettings reloaded = (AudioSettings) load.invoke(null);
      assertEquals(MusicPlaylist.world(healthy).tracks(), MusicPlaylist.world(reloaded).tracks());
      Field problem = AudioConfig.class.getDeclaredField("loadProblem");
      problem.setAccessible(true);
      assertEquals("", problem.get(null));
    } finally {
      try { PowerConfig.flushPending(); }
      finally { restore(audio); restore(storage); }
    }
  }
}
