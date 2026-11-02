package local.luke.power.audio;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MusicTrackUrlsTest {
  @TempDir Path directory;

  @Test void encodedFilesMatchNativeUrlsWithSpacesUnicodeAndUriPunctuation() throws Exception {
    Path source = directory.resolve("song #50% café+.wav");
    Files.write(source, new byte[]{1});
    assertEquals(source.toUri().toURL().toExternalForm(), MusicTrackUrls.key(source));
    assertEquals(source, Path.of(MusicTrackUrls.of(source).toURI()));
    assertTrue(MusicTrackUrls.key(source).contains("%20"));
    assertTrue(MusicTrackUrls.key(source).contains("%23"));
    assertTrue(MusicTrackUrls.key(source).contains("%25"));
  }

  @Test void nonexistentFilesAndNormalizedRelativePathsNeedNoDiscovery() throws Exception {
    Path source = directory.resolve("missing/../song.wav");
    assertEquals(directory.resolve("song.wav"), Path.of(MusicTrackUrls.of(source).toURI()));
    assertEquals("file", MusicTrackUrls.of(source).getProtocol());
    assertFalse(Files.exists(directory.resolve("song.wav")));
  }
}
