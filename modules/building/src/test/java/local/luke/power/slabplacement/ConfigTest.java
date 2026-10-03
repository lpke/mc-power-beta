package local.luke.power.slabplacement;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import local.luke.power.slabplacement.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
  @TempDir Path directory;

  @Test
  void savesAndReloadsWithoutTemporaryFiles() throws Exception {
    Path file = directory.resolve("nested/config.properties");
    var store = new ConfigStore(file);
    var s = store.load();
    s.enabled = !s.enabled;
    store.save(s);
    assertEquals(s.enabled, store.load().enabled);
    try (var files = Files.list(file.getParent())) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void invalidFileIsPreserved() throws Exception {
    Path file = directory.resolve("config.properties");
    Files.writeString(file, "enabled=bad\n");
    assertThrows(IOException.class, () -> new ConfigStore(file).load());
    assertEquals("enabled=bad\n", Files.readString(file));
    Files.writeString(file, "x".repeat(65537));
    assertThrows(IOException.class, () -> new ConfigStore(file).load());
  }

  @Test
  void writeFailurePreservesExistingFile() throws Exception {
    Path file = directory.resolve("parent");
    Files.writeString(file, "keep");
    assertThrows(
        IOException.class, () -> new ConfigStore(file.resolve("settings")).save(new Settings()));
    assertEquals("keep", Files.readString(file));
  }
}
