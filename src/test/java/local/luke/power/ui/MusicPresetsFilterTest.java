package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import local.luke.power.audio.MusicPlaylist;
import org.junit.jupiter.api.Test;

class MusicPresetsFilterTest {
  @Test void nameSearchPreservesCatalogOrderAndIgnoresCaseAndSurroundingSpaces() {
    var first = new MusicPlaylist("first", "Evening rain", List.of(), Map.of());
    var second = new MusicPlaylist("second", "RAIN", List.of(), Map.of());
    var third = new MusicPlaylist("third", "Daytime", List.of(), Map.of());
    var catalog = List.of(first, third, second);
    assertEquals(List.of(first, second), MusicPresetsPanel.filterPresets(catalog, "  RaIn  "));
    assertEquals(catalog, MusicPresetsPanel.filterPresets(catalog, " "));
    assertTrue(MusicPresetsPanel.filterPresets(catalog, "absent").isEmpty());
    assertEquals(List.of(first, third, second), catalog);
  }
}
