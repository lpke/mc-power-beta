package local.luke.power.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MusicDataTest {
  @TempDir Path directory;
  private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }

  @Test void splitAndMergePreserveMusicAndPreferencesWithoutDuplicatingOwnership() {
    JsonObject audio = json("""
        {"master":71,"playlist":"quiet","menuPlaylist":"@world",
         "playlists":[{"id":"quiet","name":"Quiet","tracks":["music:missing","music:a"],"groupVolumes":{"folder:/missing":20}}],
         "playlistTracks":["music:a","music:missing"],"menuPlaylistTracks":[],"disabledTracks":["music:default"],
         "favourites":["music:missing"],"groupVolumes":{"World":40},"menuGroupVolumes":{},
         "musicDirectories":["/missing"],"menuDirectories":["/missing"],
         "disabledMusicDirectories":["/missing"],"disabledMenuDirectories":[],
         "sounds":{"random.click":33,"music:missing":100}}
        """);
    JsonObject music = MusicData.split(audio), preferences = MusicData.preferences(audio);
    JsonObject sidecar = MusicData.empty(); sidecar.add("audio", music); MusicData.validate(sidecar);
    assertEquals(audio, MusicData.merge(preferences, music));
    assertFalse(preferences.has("playlists")); assertFalse(preferences.has("musicDirectories"));
    assertEquals(json("{\"random.click\":33}"), preferences.getAsJsonObject("sounds"));
    assertEquals(json("{\"music:missing\":100}"), music.getAsJsonObject("sounds"));
    assertEquals("music:a", music.getAsJsonArray("playlistTracks").get(0).getAsString());
  }

  @Test void queueKeepsOrderDuplicatesAndMissingTrackIds() {
    JsonObject data = MusicData.empty(); data.add("queue", json("{\"tracks\":[\"music:missing\",\"music:a\",\"music:missing\"]}"));
    MusicData.validate(data);
    assertEquals("music:missing", data.getAsJsonObject("queue").getAsJsonArray("tracks").get(2).getAsString());
  }

  @Test void strictFormatsRejectUnknownFieldsDuplicateMembershipAndInvalidValues() {
    for (String bad : new String[]{
        "{\"formatVersion\":2,\"audio\":{},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1.5,\"audio\":{},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1,\"audio\":{\"future\":true},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1,\"audio\":{\"playlistTracks\":[\"music:a\",\"music:a\"]},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1,\"audio\":{\"sounds\":{\"random.click\":50}},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1,\"audio\":{\"groupVolumes\":{\"World\":101}},\"queue\":{\"tracks\":[]}}",
        "{\"formatVersion\":1,\"audio\":{\"disabledMusicDirectories\":[\"/gone\"]},\"queue\":{\"tracks\":[]}}"})
      assertThrows(IllegalArgumentException.class, () -> MusicData.validate(json(bad)), bad);
    JsonObject data = MusicData.empty(); JsonArray tracks = new JsonArray();
    for (int i = 0; i < 257; i++) tracks.add("music:a"); data.getAsJsonObject("queue").add("tracks", tracks);
    assertThrows(IllegalArgumentException.class, () -> MusicData.validate(data));
    assertThrows(IllegalArgumentException.class, () -> MusicData.validatePreferences(json("{\"preset\":\"old\"}")));
    assertThrows(IllegalArgumentException.class, () -> MusicData.validatePreferences(json("{\"playlistTracks\":[]}")));
  }

  @Test void durableReadWriteAndVerifiedBackupPreserveExactBytes() throws Exception {
    Path file = directory.resolve("power-beta/music.json");
    JsonObject data = MusicData.empty(); MusicData.write(file, data);
    byte[] original = Files.readAllBytes(file); MusicData.backup(file, "music-test-");
    data.add("queue", json("{\"tracks\":[\"music:a\",\"music:a\"]}")); MusicData.write(file, data);
    assertEquals(data, MusicData.read(file));
    try (var files = Files.list(file.getParent().resolve("backups"))) {
      assertTrue(files.anyMatch(backup -> { try { return Arrays.equals(original, Files.readAllBytes(backup)); } catch (Exception e) { throw new RuntimeException(e); } }));
    }
    try (var files = Files.list(file.getParent())) { assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp"))); }
  }

  @Test void missingCorruptFutureOversizedAndLinkedFilesCannotBeOverwritten() throws Exception {
    Path file = directory.resolve("music.json");
    assertThrows(java.io.IOException.class, () -> MusicData.read(file));
    for (String original : new String[]{"broken", "{\"formatVersion\":99,\"audio\":{},\"queue\":{\"tracks\":[]}}", " ".repeat(MusicData.MAX_BYTES + 1)}) {
      Files.writeString(file, original);
      assertThrows(java.io.IOException.class, () -> MusicData.read(file));
      assertThrows(java.io.IOException.class, () -> MusicData.write(file, MusicData.empty()));
      assertEquals(original, Files.readString(file));
    }
    Files.delete(file); Path target = directory.resolve("target"); Files.writeString(target, "keep"); Files.createSymbolicLink(file, target);
    assertThrows(java.io.IOException.class, () -> MusicData.write(file, MusicData.empty())); assertEquals("keep", Files.readString(target));
    Files.delete(file); Files.createSymbolicLink(file, directory.resolve("missing"));
    assertThrows(java.io.IOException.class, () -> MusicData.write(file, MusicData.empty()));
    Path linkedDirectory = directory.resolve("linked"); Files.createSymbolicLink(linkedDirectory, directory);
    assertThrows(java.io.IOException.class, () -> MusicData.write(linkedDirectory.resolve("new.json"), MusicData.empty()));
  }
  @org.junit.jupiter.api.Test void menuSelectionCannotUseTheWorldOnlyEmptySentinel() {
    var preferences = new com.google.gson.JsonObject();
    preferences.addProperty("playlist", ""); preferences.addProperty("menuPlaylist", "");
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> MusicData.validatePreferences(preferences));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> MusicData.validateSelections(preferences, MusicData.empty().getAsJsonObject("audio")));
    preferences.addProperty("menuPlaylist", "@world");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> MusicData.validatePreferences(preferences));
  }
}
