package local.luke.power.audio;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuiltinMusicTest {
  @Test void modesContainExactlyThePinnedOverworldTracks() {
    assertEquals(21, BuiltinMusic.TRACKS.size());
    assertEquals(12, count(AudioSettings.MusicMode.VANILLA));
    assertEquals(18, count(AudioSettings.MusicMode.ALPHA_BETA));
    assertEquals(21, count(AudioSettings.MusicMode.ALL_MINECRAFT));
    assertEquals("creative4.ogg (Aria Math)", BuiltinMusic.label("creative4.ogg"));
    assertEquals("axolotl.ogg", BuiltinMusic.label("axolotl.ogg"));
    assertEquals("dragon_fish.ogg", BuiltinMusic.label("dragon_fish.ogg"));
    assertEquals("shuniji.ogg", BuiltinMusic.label("shuniji.ogg"));
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
