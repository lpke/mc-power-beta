package local.luke.power.storage;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MusicDataUpgradeTest {
  @TempDir Path directory;

  private static JsonObject previous() {
    return JsonParser.parseString("""
        {"formatVersion":2,"audio":{
          "playlists":[{"id":"one","name":"One","tracks":["music:z","music:missing","music:a"],
            "groupVolumes":{"Alpha":37,"Beta / Creative":100}}],
          "favourites":["music:z","music:missing","music:a"],
          "musicDirectories":["relative/../music","/personal/missing"],
          "disabledMusicDirectories":["/personal/missing"],
          "sounds":{"music:z":29,"music:missing":100}},
         "queue":{"tracks":["music:missing","music:z","music:missing"]}}
        """).getAsJsonObject();
  }

  @Test void conversionMovesGainsToLibraryAndPreservesMembershipAndQueue() throws Exception {
    JsonObject original = previous(), untouched = original.deepCopy();
    JsonObject next = MusicDataUpgrade.convert(original);
    JsonObject expected = original.deepCopy();
    expected.addProperty("formatVersion", 4);
    var expectedAudio = expected.getAsJsonObject("audio");
    expectedAudio.getAsJsonArray("playlists").get(0).getAsJsonObject().remove("groupVolumes");
    expectedAudio.add("libraryGroupVolumes", JsonParser.parseString("{\"Alpha\":37}"));
    expectedAudio.add("playlistOrder", JsonParser.parseString("[\"beta\",\"favorites\",\"one\"]"));
    expected.getAsJsonObject("audio").remove("disabledMusicDirectories");
    assertEquals(expected, next);
    assertEquals(untouched, original);
    assertDoesNotThrow(() -> MusicData.validate(next));
    assertThrows(IllegalArgumentException.class, () -> MusicData.validate(original));
  }

  @Test void formatThreeConversionPreservesLibraryOrderAndMergesOnlyConsistentNondefaultGains() {
    JsonObject previous = previous(); previous.addProperty("formatVersion", 3);
    JsonObject audio = previous.getAsJsonObject("audio"); audio.remove("disabledMusicDirectories");
    audio.add("libraryOrder", JsonParser.parseString("[\"Alpha\",\"source:/personal/missing\"]"));
    audio.add("favoritesGroupVolumes", JsonParser.parseString("{\"Beta / Creative\":23}"));
    JsonObject converted = MusicDataUpgrade.convert(previous);
    assertEquals(audio.get("libraryOrder"), converted.getAsJsonObject("audio").get("libraryOrder"));
    assertEquals(JsonParser.parseString("{\"Alpha\":37,\"Beta / Creative\":23}"),
        converted.getAsJsonObject("audio").get("libraryGroupVolumes"));
    assertFalse(converted.getAsJsonObject("audio").has("favoritesGroupVolumes"));
    audio.getAsJsonObject("favoritesGroupVolumes").addProperty("Alpha", 50);
    JsonObject unchanged = previous.deepCopy();
    assertThrows(IllegalArgumentException.class, () -> MusicDataUpgrade.convert(previous));
    assertEquals(unchanged, previous);
  }

  @Test void physicalFolderGainsRequireAnExplicitSourceMappingBeforeAnyReplacement() throws Exception {
    JsonObject previous = previous();
    previous.getAsJsonObject("audio").getAsJsonArray("playlists").get(0).getAsJsonObject()
        .getAsJsonObject("groupVolumes").addProperty("folder:/personal/album", 37);
    Path file = directory.resolve("music.json"); Files.writeString(file, previous.toString());
    byte[] unchanged = Files.readAllBytes(file);
    var failure = assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(file));
    assertTrue(failure.getMessage().contains("explicit Library source mapping"));
    assertArrayEquals(unchanged, Files.readAllBytes(file));
  }

  @Test void explicitDiskConversionVerifiesBackupAndLeavesConfigurationAndOtherFilesIdentical() throws Exception {
    Path music = directory.resolve("power-beta/music.json");
    Files.createDirectories(music.getParent());
    String original = new GsonBuilder().setPrettyPrinting().create().toJson(previous()) + "\n";
    Files.writeString(music, original);
    Path config = directory.resolve("config/power-beta.json");
    Files.createDirectories(config.getParent());
    Files.writeString(config, "{\"schemaVersion\":9,\"unrelated\":\"keep exactly\"}\n");
    Path song = directory.resolve("personal.ogg"); Files.writeString(song, "keep source audio bytes");
    byte[] configBytes = Files.readAllBytes(config), songBytes = Files.readAllBytes(song);
    Path backup = MusicDataUpgrade.convert(music);
    assertEquals(original, Files.readString(backup));
    assertEquals(MusicDataUpgrade.convert(previous()), MusicData.read(music));
    assertArrayEquals(configBytes, Files.readAllBytes(config));
    assertArrayEquals(songBytes, Files.readAllBytes(song));
    try (var paths = Files.list(music.getParent())) {
      assertFalse(paths.anyMatch(path -> path.toString().endsWith(".tmp")));
    }
    byte[] converted = Files.readAllBytes(music);
    assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music));
    assertArrayEquals(converted, Files.readAllBytes(music));
  }

  @Test void unsupportedMalformedAndReservedDataCannotBePartlyConverted() throws Exception {
    Path music = directory.resolve("music.json");
    for (int version : new int[]{1, 4, 99}) {
      JsonObject value = previous(); value.addProperty("formatVersion", version);
      Files.writeString(music, value.toString());
      String original = Files.readString(music);
      assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music));
      assertEquals(original, Files.readString(music));
    }
    for (String field : new String[]{"future", "favoritesGroupVolumes", "libraryOrder"}) {
      JsonObject value = previous(); value.getAsJsonObject("audio").add(field, new JsonObject());
      assertThrows(IllegalArgumentException.class, () -> MusicDataUpgrade.convert(value));
    }
    JsonObject reserved = previous();
    reserved.getAsJsonObject("audio").getAsJsonArray("playlists").get(0).getAsJsonObject().addProperty("id", "favorites");
    Files.writeString(music, reserved.toString());
    assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music));
    assertEquals(reserved.toString(), Files.readString(music));
    Files.delete(music);
    Path target = directory.resolve("target.json"); Files.writeString(target, previous().toString());
    Files.createSymbolicLink(music, target);
    assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music));
    assertEquals(previous().toString(), Files.readString(target));
  }
  @Test void failureAfterReplacementRestoresVerifiedOriginalAndKeepsTheFailure() throws Exception {
    Path music = directory.resolve("music.json");
    byte[] original = (new GsonBuilder().setPrettyPrinting().create().toJson(previous()) + "\n")
        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(music, original);
    var injected = new java.io.IOException("Injected directory sync failure after replacement");
    var failure = assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music, (stage, file) -> {
      if (stage.equals("replaced")) {
        assertEquals(4, MusicData.read(file).get("formatVersion").getAsInt());
        throw injected;
      }
    }));
    assertSame(injected, failure.getCause());
    assertTrue(failure.getMessage().contains("Original music data restored"));
    assertTrue(failure.getMessage().contains("Verified backup:"));
    assertArrayEquals(original, Files.readAllBytes(music));
    assertBackupAndNoTemporaryFiles(music, original);
  }

  @Test void foreignDataAfterReplacementSurvivesFailedVerificationAndRollback() throws Exception {
    Path music = directory.resolve("music.json");
    byte[] original = previous().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(music, original);
    byte[] foreign = "a concurrent writer owns these bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var failure = assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music, (stage, file) -> {
      if (stage.equals("replaced")) Files.write(file, foreign);
    }));
    assertTrue(failure.getCause().getMessage().contains("Converted music verification failed"));
    assertEquals(1, failure.getCause().getSuppressed().length);
    assertTrue(failure.getCause().getSuppressed()[0].getMessage().contains("will not overwrite"));
    assertTrue(failure.getMessage().contains("Preserve the current file"));
    assertTrue(failure.getMessage().contains("Verified backup:"));
    assertArrayEquals(foreign, Files.readAllBytes(music));
    assertBackupAndNoTemporaryFiles(music, original);
  }

  @Test void rollbackRechecksOwnershipAfterPreparingItsDurableRestore() throws Exception {
    Path music = directory.resolve("music.json");
    byte[] original = previous().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(music, original);
    byte[] foreign = "new data written during rollback".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var injected = new java.io.IOException("Injected post-replacement failure");
    var failure = assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music, (stage, file) -> {
      if (stage.equals("replaced")) throw injected;
      if (stage.equals("rollback-prepared")) Files.write(file, foreign);
    }));
    assertSame(injected, failure.getCause());
    assertEquals(1, injected.getSuppressed().length);
    assertArrayEquals(foreign, Files.readAllBytes(music));
    assertBackupAndNoTemporaryFiles(music, original);
  }

  @Test void rollbackFailureRetainsBothErrorsAndTheVerifiedBackup() throws Exception {
    Path music = directory.resolve("music.json");
    byte[] original = previous().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(music, original);
    var injected = new java.io.IOException("Injected post-replacement failure");
    var rollback = new java.io.IOException("Injected rollback I/O failure");
    var failure = assertThrows(java.io.IOException.class, () -> MusicDataUpgrade.convert(music, (stage, file) -> {
      if (stage.equals("replaced")) throw injected;
      if (stage.equals("rollback-prepared")) throw rollback;
    }));
    assertSame(injected, failure.getCause());
    assertArrayEquals(new Throwable[]{rollback}, injected.getSuppressed());
    assertTrue(failure.getMessage().contains("Automatic rollback could not complete"));
    assertTrue(failure.getMessage().contains("Verified backup:"));
    assertEquals(MusicDataUpgrade.convert(previous()), MusicData.read(music));
    assertBackupAndNoTemporaryFiles(music, original);
  }

  private static void assertBackupAndNoTemporaryFiles(Path music, byte[] original) throws Exception {
    try (var paths = Files.list(music.getParent().resolve("backups"))) {
      var backups = paths.toList();
      assertEquals(1, backups.size());
      assertArrayEquals(original, Files.readAllBytes(backups.get(0)));
    }
    try (var paths = Files.list(music.getParent())) {
      assertFalse(paths.anyMatch(path -> path.toString().endsWith(".tmp")));
    }
  }

}
