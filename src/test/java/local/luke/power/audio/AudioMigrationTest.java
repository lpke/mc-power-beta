package local.luke.power.audio;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioMigrationTest {
  @Test void oldPoliciesBecomeOneSetWithoutLosingTrackOrQueuePreferences() {
    var doc = JsonParser.parseString("""
        {"settings":{"native":{"music":"0.75","sound":"0.8"},
        "audio":{"musicMode":"ADD","sounds":{"music:calm1.ogg":42},"musicDirectories":["my music"]},
        "musicQueue":{"tracks":["music:calm1.ogg"]},
        "power_environment:config":{"MUSIC_CONFIG":{"disableBackgroundMusic":true,
        "disableDefaultMinecraftBGM":true,"mainMenuThemeEnabled":true,
        "stopDimensionSpecificSongOnPortalUse":true,
        "musicCoundownRandomIntervalMin":100,"musicCoundownRandomIntervalMax":200,
        "volumeRainAmbient":0.5}}}}
        """).getAsJsonObject();
    var before = doc.deepCopy();
    AudioMigration.upgrade(doc);
    var all = doc.getAsJsonObject("settings"); var audio = all.getAsJsonObject("audio");
    assertEquals("VANILLA", audio.get("musicMode").getAsString());
    assertEquals("ONLY", audio.get("customMusic").getAsString());
    assertEquals("CUSTOM", audio.get("menuMusic").getAsString());
    assertEquals("STOP_SPECIFIC", audio.get("dimensionMusic").getAsString());
    assertEquals(5, audio.get("gapMinSeconds").getAsInt());
    assertEquals(15, audio.get("gapMaxSeconds").getAsInt());
    assertEquals("0.0", all.getAsJsonObject("native").get("music").getAsString());
    assertEquals("0.8", all.getAsJsonObject("native").get("sound").getAsString());
    assertEquals(before.getAsJsonObject("settings").get("musicQueue"), all.get("musicQueue"));
    assertEquals(42, audio.getAsJsonObject("sounds").get("music:calm1.ogg").getAsInt());
    assertEquals(1, all.getAsJsonObject("power_environment:config").getAsJsonObject("MUSIC_CONFIG").size());
    var once = doc.deepCopy(); AudioMigration.upgrade(doc); assertEquals(once, doc);
  }

  @Test void defaultsAreTenToTwentyMinutesButStoredInSeconds() {
    var doc = JsonParser.parseString("{\"settings\":{}}").getAsJsonObject();
    AudioMigration.upgrade(doc);
    var settings = new Gson().fromJson(doc.getAsJsonObject("settings").get("audio"), AudioSettings.class);
    settings.validate();
    assertEquals(600, settings.gapMinSeconds); assertEquals(1200, settings.gapMaxSeconds);
    assertEquals(AudioSettings.MusicMode.VANILLA, settings.musicMode);
  }

  @Test void rejectReversedAndOverflowingRanges() {
    var s = new AudioSettings(); s.gapMinSeconds = 1201;
    assertThrows(IllegalArgumentException.class, s::validate);
    s.gapMinSeconds = 0; s.gapMaxSeconds = Integer.MAX_VALUE;
    assertThrows(IllegalArgumentException.class, s::validate);
    s.gapMaxSeconds = 0; assertDoesNotThrow(s::validate);
  }
}
