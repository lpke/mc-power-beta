package local.luke.power.audio;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuiltinMusicTest {
  @Test void modesContainThePinnedSoundtrackGroups() {
    assertEquals(83, BuiltinMusic.TRACKS.size());
    assertEquals(12, count(AudioSettings.MusicMode.VANILLA));
    assertEquals(35, count(AudioSettings.MusicMode.ALPHA_BETA));
    assertEquals(83, count(AudioSettings.MusicMode.ALL_MINECRAFT));
    assertEquals("creative4.ogg (Aria Math)", BuiltinMusic.label("creative4.ogg"));
    assertEquals("axolotl.ogg", BuiltinMusic.label("axolotl.ogg"));
    assertEquals("dragon_fish.ogg", BuiltinMusic.label("dragon_fish.ogg"));
    assertEquals("shuniji.ogg", BuiltinMusic.label("shuniji.ogg"));
  }
  @Test void creativeTracksHaveASeparateGroupAndHistoricalIdsStayStable() {
    assertEquals(6, BuiltinMusic.TRACKS.stream().filter(BuiltinMusic.Track::creative).count());
    assertEquals("Beta / Creative", BuiltinMusic.find("music:creative4.ogg").group());
    assertEquals("Alpha", BuiltinMusic.find("music:calm1.mus").group());
    assertEquals(java.util.List.of("Alpha", "Beta / Creative", "Update Aquatic", "Caves & Cliffs",
        "The Wild Update", "Trails & Tales", "Tricky Trials", "Chase the Skies", "Chaos Cubed"),
        BuiltinMusic.TRACKS.stream().filter(BuiltinMusic.Track::background).map(BuiltinMusic.Track::group).distinct().toList());
    assertEquals("Chaos Cubed", BuiltinMusic.find("music:shores.ogg").group());
    assertEquals(56, BuiltinMusic.TRACKS.stream().filter(BuiltinMusic.Track::background).count());
    assertEquals(83, BuiltinMusic.TRACKS.stream().map(BuiltinMusic.Track::id).distinct().count());
    assertEquals("Beta / Menu", BuiltinMusic.find("music:mutation.ogg").group());
    assertEquals("Beta / Credits", BuiltinMusic.find("music:alpha.ogg").group());
    assertEquals("Tricky Trials / Records", BuiltinMusic.find("music:creator.ogg").group());
  }
  private long count(AudioSettings.MusicMode mode) {
    return BuiltinMusic.TRACKS.stream().filter(t -> t.included(mode)).count();
  }
  @Test void everyBundledFileMatchesTheOfficialAssetIndex() throws Exception {
    for (var track : BuiltinMusic.TRACKS) try (var in = getClass().getResourceAsStream("/assets/powerbeta/music/" + track.file())) {
      assertNotNull(in, track.file());
      byte[] data = in.readAllBytes();
      assertEquals(track.size(), data.length, track.file());
      assertEquals(track.sha1(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data)), track.file());
    }
  }
}
