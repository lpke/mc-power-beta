package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PanoramaCatalogTest {
  @Test void pickerListsEachSceneOnceAndStartsWithTheDirtBackground() {
    var entries = PanoramaCatalog.entries();
    assertEquals(PanoramaCatalog.DIRT, entries.get(0).id());
    assertEquals("", entries.get(0).folder());
    assertEquals(entries.size(), entries.stream().map(PanoramaCatalog.Entry::scene).distinct().count());
    assertEquals(15, entries.size());
    assertTrue(entries.stream().anyMatch(e -> e.id().equals("1_13")));
    assertTrue(entries.stream().anyMatch(e -> e.id().equals("26_3")));
  }
  @Test void oldSelectionsKeepTheirTexturesAndMapToTheirSharedScene() {
    for (String id : List.of("1_0", "1_1", "1_2_1", "1_3_1", "1_4_2", "1_5_1", "1_6_1", "1_7_2", "1_8", "1_9", "1_10", "1_11", "1_12")) {
      assertEquals("b1_8_1", PanoramaCatalog.scene(id), id);
      assertEquals("Beta 1.8.1", PanoramaCatalog.label(id), id);
      assertTrue(PanoramaCatalog.folder(id).startsWith("official_"), id);
      assertFalse(PanoramaCatalog.entries().stream().anyMatch(e -> e.id().equals(id)), id);
    }
    assertEquals("official_1_0", PanoramaCatalog.folder("1_4_2"));
    assertEquals("official_1_5_1", PanoramaCatalog.folder("1_8"));
    assertEquals("official_b1_8_1", PanoramaCatalog.folder("beta18"));
    assertEquals("b1_8_1", PanoramaCatalog.scene("official_1_0"));
    assertEquals("custom_scene", PanoramaCatalog.folder("custom_scene"));
  }
}
