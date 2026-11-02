package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class Mp3ConverterTest {
  @Test void configuredBatchConvertsBeyondOldLimitAndDeduplicatesPaths() {
    var tracks = java.util.stream.IntStream.range(0, 2048)
        .mapToObj(i -> new MusicLibrary.Track(Path.of("song-" + i + ".mp3"), "song-" + i + ".mp3", null)).toList();
    assertEquals(500, Mp3Converter.batch(tracks, 500).size());
    assertEquals(2000, Mp3Converter.batch(tracks, 2000).size());
    assertEquals(1, Mp3Converter.batch(List.of(tracks.get(0), tracks.get(0)), 500).size());
    assertThrows(IllegalArgumentException.class, () -> Mp3Converter.batch(tracks, 0));
  }
  @Test void missingConverterDisablesConversionWithoutThrowing() {
    assertFalse(Mp3Converter.probeExecutable("/nonexistent-power-beta-test/ffmpeg"));
  }

  @Test void conversionIsOfferedOnlyForUnconvertedMp3Files() {
    var ogg=new MusicLibrary.Track(Path.of("song.ogg"),"song.ogg");
    var ready=new MusicLibrary.Track(Path.of("song.mp3"),"song.mp3",Path.of("cache.wav"));
    var waiting=new MusicLibrary.Track(Path.of("song.MP3"),"song.MP3",null);
    assertFalse(Mp3Converter.needed(List.of()));
    assertFalse(Mp3Converter.needed(List.of(ogg,ready)));
    assertTrue(Mp3Converter.needed(List.of(ogg,ready,waiting)));
  }
}
