package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import local.luke.power.audio.MusicLibrarySources;
import org.junit.jupiter.api.Test;

class MissingSourceWarningsTest {
  private static MusicLibrarySources.Folder folder(String path, boolean available) {
    return new MusicLibrarySources.Folder("source:" + path, path, "music", available ? "Available" : "Unavailable", List.of(), List.of());
  }
  @Test void distinctMissingRootsWithTheSameNameAreAllShownOnceWithoutFilesystemChecks() {
    var shown = new HashSet<String>(); var warnings = new MissingSourceWarnings(shown);
    var sources = List.of(folder("/old/one/music", false), folder("/old/two/music", false), folder("/old/three/music", false));
    warnings.observe(sources); assertEquals(3, warnings.remaining());
    assertEquals("/old/one/music", warnings.next()); warnings.observe(sources);
    assertEquals(2, warnings.remaining()); assertEquals("/old/two/music", warnings.next());
    assertEquals("/old/three/music", warnings.next()); assertNull(warnings.next());
    warnings.observe(sources); assertEquals(0, warnings.remaining());
    warnings.reset(); warnings.observe(sources); assertEquals(3, warnings.remaining());
  }
  @Test void rescansDropResolvedPendingRootsAndClosingAnEditorDoesNotSilenceUnseenWarnings() {
    var shown = new HashSet<String>(); var first = new MissingSourceWarnings(shown);
    first.observe(List.of(folder("/one/music", false), folder("/two/music", false)));
    assertEquals("/one/music", first.next());
    var reopened = new MissingSourceWarnings(shown);
    reopened.observe(List.of(folder("/one/music", false), folder("/two/music", false)));
    assertEquals("/two/music", reopened.next());
    first.observe(List.of(folder("/one/music", false), folder("/two/music", false)));
    assertEquals(0, first.remaining(), "another editor already showed this queued source");
    first.observe(List.of(folder("/one/music", false), folder("/two/music", true)));
    assertEquals(0, first.remaining());
    first.observe(List.of(folder("/new/music", false))); assertEquals("/new/music", first.next());
  }
}
