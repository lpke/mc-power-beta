package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class AudioTest {
  @TempDir Path dir;

  @Test
  void dimensionAndBiomeMusicRulesPreserveUntaggedTracks() {
    assertTrue(TrackRules.eligible("calm1.ogg", -1, null));
    assertTrue(TrackRules.eligible("song-nether-specific.ogg", -1, "Hell"));
    assertFalse(TrackRules.eligible("song-nether-specific.ogg", 0, "Forest"));
    assertTrue(TrackRules.eligible("song-forest-specific.ogg", 0, "Forest"));
    assertFalse(TrackRules.eligible("song-forest-specific.ogg", 0, null));
    assertTrue(TrackRules.eligible("song-level2-specific.ogg", 2, null));
    assertTrue(TrackRules.dimensionSpecific("song-overworld-specific.ogg", 0));
    assertFalse(TrackRules.dimensionSpecific("song-forest-specific.ogg", 0));
  }

  @Test
  void volumeIsMultiplicativeAndMuteIsAbsolute() {
    AudioSettings s = new AudioSettings();
    s.master = 50;
    s.categories.put("hostile", 40);
    s.sounds.put("mob.zombie", 25);
    assertEquals(.05, s.gain("mob.zombie", false), 1e-9);
    s.master = 0;
    assertEquals(0, s.gain("mob.zombie", false));
  }

  @Test
  void soundCategoriesCoverBetaFamilies() {
    assertEquals("weather", AudioSettings.category("ambient.weather.rain", false));
    assertEquals("passive", AudioSettings.category("mob.cow.say", false));
    assertEquals("hostile", AudioSettings.category("mob.ghast.moan", false));
    assertEquals("blocks", AudioSettings.category("step.stone", false));
    assertEquals("interface", AudioSettings.category("random.click", true));
    assertEquals("records", AudioSettings.category("records.cat", false));
    assertEquals("passive", AudioSettings.category("power_controls:entity.sheep.shear", false));
    assertEquals("blocks", AudioSettings.category("power_controls:random.chestopen", false));
  }

  @Test
  void invalidVolumesFailBeforeSaving() {
    AudioSettings s = new AudioSettings();
    s.master = 101;
    assertThrows(IllegalArgumentException.class, s::validate);
    s.master = 100;
    s.sounds.put("x", -1);
    assertThrows(IllegalArgumentException.class, s::validate);
  }

  @Test
  void musicVolumesAcceptRealFilenames() {
    AudioSettings s = new AudioSettings();
    s.sounds.put("music:My song - été (1).ogg", 75);
    assertDoesNotThrow(s::validate);
    s.sounds.put("music:bad\nname.ogg", 50);
    assertThrows(IllegalArgumentException.class, s::validate);
  }

  @Test
  void scanDeduplicatesAndSkipsBadAndUnsupportedFiles() throws Exception {
    Files.write(dir.resolve("good.ogg"), new byte[] {'O', 'g', 'g', 'S', 0, 0, 0, 0, 0, 0, 0, 0});
    Files.writeString(dir.resolve("broken.ogg"), "not an audio stream");
    Files.writeString(dir.resolve("track.mp3"), "not supported");
    var scan = MusicLibrary.scan(dir, List.of(".", dir.toString()), false);
    assertEquals(1, scan.tracks().size());
    assertEquals(4, scan.warnings().size());
    assertTrue(Files.exists(dir.resolve("broken.ogg")));
  }

  @Test
  void mp3IsListedForConversionAndEqualFilenamesHaveSeparateIds() throws Exception {
    Path a = Files.createDirectories(dir.resolve("a")), b = Files.createDirectories(dir.resolve("b"));
    byte[] mp3 = {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0, 0, 0};
    Files.write(a.resolve("same.mp3"), mp3); Files.write(b.resolve("same.mp3"), mp3);
    var scan = MusicLibrary.scan(dir, List.of("a", "b", "a"), false);
    assertEquals(2, scan.tracks().size());
    assertNotEquals(scan.tracks().get(0).id(), scan.tracks().get(1).id());
    assertFalse(scan.tracks().get(0).playable());
    assertArrayEquals(mp3, Files.readAllBytes(a.resolve("same.mp3")));
  }

  @Test
  void queuePreservesDuplicatesAndRejectsOverflow() {
    MusicQueue q = new MusicQueue(); q.add("music:a"); q.add("music:b"); q.add("music:a");
    q.move(2, -1); assertEquals(List.of("music:a", "music:a", "music:b"), q.tracks);
    q.move(-1, 1); q.move(0, -1); q.move(2, 1); q.validate();
    while (q.tracks.size() < 256) q.add("music:a");
    assertThrows(IllegalArgumentException.class, () -> q.add("music:b"));
    assertEquals(256, q.tracks.size());
  }

  @Test
  void recursionIsOptInAndSymlinksAreNotFollowed() throws Exception {
    Path sub = Files.createDirectory(dir.resolve("sub"));
    Files.write(sub.resolve("song.ogg"), new byte[] {'O', 'g', 'g', 'S', 0, 0, 0, 0, 0, 0, 0, 0});
    Files.createSymbolicLink(dir.resolve("loop"), dir);
    assertTrue(MusicLibrary.scan(dir, List.of("."), false).tracks().isEmpty());
    assertEquals(1, MusicLibrary.scan(dir, List.of("."), true).tracks().size());
  }

  @Test
  void missingDirectoriesDoNotEraseExistingFiles() {
    var scan = MusicLibrary.scan(dir, List.of("missing"), true);
    assertTrue(scan.tracks().isEmpty());
    assertEquals(1, scan.warnings().size());
    assertFalse(Files.exists(dir.resolve("missing")));
  }

  @Test
  void selectorAvoidsRepeatsAndHandlesEmptyOrSingletonLibraries() {
    TrackSelector s = new TrackSelector();
    Random r = new Random(1);
    assertNull(s.choose(List.<String>of(), true, true, r, x -> x));
    assertEquals("only", s.choose(List.of("only"), true, true, r, x -> x));
    String last = "";
    for (int i = 0; i < 10000; i++) {
      String next = s.choose(List.of("a", "b", "c"), true, true, r, x -> x);
      assertNotEquals(last, next);
      last = next;
    }
  }

  @Test
  void sequentialSelectionIsStable() {
    TrackSelector s = new TrackSelector();
    Random r = new Random(1);
    for (int i = 0; i < 10; i++)
      assertEquals(
          List.of("a", "b", "c").get(i % 3),
          s.choose(List.of("a", "b", "c"), false, true, r, x -> x));
  }
}
