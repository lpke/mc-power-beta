package local.luke.power.fastplace;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import local.luke.power.fastplace.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
  @TempDir Path directory;

  @Test
  void defaultsAndRoundTrip() throws Exception {
    var store = new ConfigStore(directory.resolve("nested/settings.properties"));
    var s = store.load();
    assertFalse(s.enabled);
    assertEquals(local.luke.power.fastplace.SlabMode.DOUBLE, s.slabMode);
    assertTrue(s.restrictionEnabled);
    assertTrue(s.rememberOrientation);
    assertEquals(RestrictionMode.FACE, s.restrictionMode);
    assertEquals(2, s.attemptsPerTick);
    s.setEnabled(true);
    s.attemptsPerTick = 16;
    s.slabMode = local.luke.power.fastplace.SlabMode.MATCH_FIRST;
    s.listMode = Settings.ListMode.WHITELIST;
    s.whitelist = Settings.parseFilters("1, 44:2");
    store.save(s);
    var loaded = store.load();
    assertTrue(loaded.enabled);
    assertEquals(local.luke.power.fastplace.SlabMode.MATCH_FIRST, loaded.slabMode);
    assertEquals(16, loaded.attemptsPerTick);
    assertEquals(s.whitelist, loaded.whitelist);
    try (var files = Files.list(directory.resolve("nested"))) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void listsAreStrictAndSupportMetadata() {
    var s = new Settings();
    s.listMode = Settings.ListMode.WHITELIST;
    assertFalse(s.permits(1, 0));
    s.whitelist = Settings.parseFilters("1,44:2,1");
    assertEquals(2, s.whitelist.size());
    assertTrue(s.permits(1, 7));
    assertTrue(s.permits(44, 2));
    assertFalse(s.permits(44, 3));
    for (String invalid : List.of("-1", "foo", "44:", "44:-1", "1,,2", "1:2:3", "99999999999"))
      assertThrows(IllegalArgumentException.class, () -> Settings.parseFilters(invalid));
  }

  @Test
  void invalidFilesAreNotOverwrittenAndRatesBounded() throws Exception {
    Path path = directory.resolve("settings.properties");
    var store = new ConfigStore(path);
    Files.writeString(path, "enabled=potato\n");
    assertThrows(IOException.class, store::load);
    assertEquals("enabled=potato\n", Files.readString(path));
    Files.writeString(path, "fastBlockPlacementCount=2000\n");
    assertEquals(16, store.load().attemptsPerTick);
    Files.writeString(path, "fastBlockPlacementCount=-2\n");
    assertEquals(1, store.load().attemptsPerTick);
    Files.writeString(path, "x".repeat(65537));
    assertThrows(IOException.class, store::load);
  }

  @Test
  void tiedAndIndependentRestriction() {
    var s = new Settings();
    s.setEnabled(false);
    assertFalse(s.restrictionEnabled);
    s.setEnabled(true);
    assertTrue(s.restrictionEnabled);
    s.restrictionTiedToFast = false;
    s.restrictionEnabled = false;
    s.setEnabled(true);
    assertFalse(s.restrictionEnabled);
  }

  @Test
  void savingFailureDoesNotDestroyExistingFile() throws Exception {
    Path parent = directory.resolve("file");
    Files.writeString(parent, "existing");
    assertThrows(
        IOException.class, () -> new ConfigStore(parent.resolve("settings")).save(new Settings()));
    assertEquals("existing", Files.readString(parent));
  }
}
